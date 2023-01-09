package com.pepul.app.pepulliv.feature.stream.presentation.streamlist

import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.pepul.app.pepulliv.*
import com.pepul.app.pepulliv.commons.presentation.dialog.MenuItem
import com.pepul.app.pepulliv.commons.presentation.dialog.MenuItemColor
import com.pepul.app.pepulliv.commons.presentation.dialog.MenuSheetDialog
import com.pepul.app.pepulliv.databinding.FragmentStreamListBinding
import com.pepul.app.pepulliv.di.ApplicationDependencies
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.StreamDto
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.WOWZStreamDto
import com.pepul.app.pepulliv.feature.stream.presentation.publish.PublishFragment
import com.pepul.app.pepulliv.feature.stream.presentation.publish.PublishFragment.Companion.SDK_TYPE_HAISHINKIT
import com.pepul.app.pepulliv.feature.stream.presentation.streamlist.dialog.GoLiveLoadingDialog
import com.pepul.app.pepulliv.feature.stream.presentation.util.StreamListAdapter
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class StreamListFragment : Fragment() {

    private var requiredPermissions = listOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO
    )

    private val viewModel: StreamListViewModel by viewModels()

    private var menuDialogReference: BottomSheetDialog? = null
    private var goLiveLoadingDialogReference: GoLiveLoadingDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_stream_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentStreamListBinding.bind(view)

        // TODO: bind state

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        /*btnPublish.setOnClickListener {
            if (checkPermission()) {
                dialogGetStreamName {
                    Log.d(TAG, "Stream name: $it")
                    val intent = Intent(this, PublishActivity::class.java)
                    intent.putExtra(Constants.EXTRA_STREAM_URL, it)
                    startActivity(intent)
                }
            } else {
                permissionsToast()
            }
        }

        btnWatch.setOnClickListener {
            if (checkPermission()) {
                dialogGetStreamName {
                    Log.d(TAG, "Stream name: $it")
                    val intent = Intent(this, WatchActivity::class.java)
                    intent.putExtra(Constants.EXTRA_STREAM_URL, it)
                    startActivity(intent)
                }
            } else {
                permissionsToast()
            }
        }*/
    }

    private fun FragmentStreamListBinding.bindState(
        uiState: StateFlow<StreamListState>,
        uiAction: (StreamListUiAction) -> Unit,
        uiEvent: SharedFlow<StreamListUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            uiEvent.collectLatest { event ->
                when (event) {
                    is StreamListUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                    is StreamListUiEvent.GotoPublish -> {
                        gotoPublish(event.stream)
                        // TODO: action loading indicator
                        goLiveLoadingDialogReference?.cancel()
                        goLiveLoadingDialogReference = null
                    }
                    is StreamListUiEvent.StreamDeleted -> {
                        context?.showToast("Stream Deleted")
                    }
                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged { old, new ->
                old.refresh == new.refresh && old.action == new.action
            }
            // .map { it.action !is LoadState.Loading && it.refresh !is LoadState.Loading }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            loadStateFlow.collectLatest { loadState ->
                Timber.d("Load State: $loadState")
                if (loadState.refresh is LoadState.NotLoading ||
                    loadState.refresh is LoadState.Error) {
                    if (swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }

                if (loadState.action is LoadState.Loading) {
                    // TODO: action loading indicator
                    if (goLiveLoadingDialogReference == null) {
                        (activity as? MainActivity)?.apply {
                            GoLiveLoadingDialog(this).also {
                                goLiveLoadingDialogReference = it
                                it.show()
                            }
                        }
                    }
                }

                if (loadState.action is LoadState.Error) {
                    // TODO: action loading indicator
                    goLiveLoadingDialogReference?.cancel()
                    goLiveLoadingDialogReference = null
                }
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            uiAction(StreamListUiAction.Refresh)
        }

        val callback = object : StreamListAdapter.Callback {
            override fun onStreamItemClick(streamItem: StreamDto) {
                gotoWatch(streamItem)
            }

            override fun onStreamDeleteClick(streamItem: StreamDto) {
                context?.showToast("Deleting ${streamItem.streamName}")
                uiAction(StreamListUiAction.DeleteStream(streamItem.streamId))
            }
        }

        val adapter = StreamListAdapter(
            context = requireContext(),
            callback = callback
        )

        bindList(
            adapter = adapter,
            uiState = uiState,
            uiAction = uiAction
        )

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )

        bindToolbar()

        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                Timber.d("onDoubleTap: $e")
                return super.onDoubleTap(e)
            }
        })
        /*streamListView.setOnTouchListener { _, e ->
            return@setOnTouchListener gestureDetector.onTouchEvent(e)
        }*/

        /*AnimationUtils.loadAnimation(requireContext(), R.anim.bubble_feedback).apply {
            repeatMode = Animation.RESTART
            repeatCount = Animation.INFINITE
            fabGoLive.startAnimation(this)
        }*/
    }

    private fun FragmentStreamListBinding.bindList(
        adapter: StreamListAdapter,
        uiState: StateFlow<StreamListState>,
        uiAction: (StreamListUiAction) -> Unit
    ) {
        streamListView.adapter = adapter

        // TODO: bind data to adapter
        val streamListFlow = uiState.map { it.streamList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            streamListFlow.collectLatest { streamList ->
                adapter.submitList(streamList)
            }
        }
        // TODO: bind scroller if necessary
    }

    private fun FragmentStreamListBinding.bindClick(
        uiState: StateFlow<StreamListState>,
        uiAction: (StreamListUiAction) -> Unit
    ) {
        fabGoLive.setOnClickListener {
            if (checkPermission()) {
                uiAction(StreamListUiAction.GetStreamKey)
            } else {
                permissionsToast()
            }
        }
    }

    private fun FragmentStreamListBinding.bindToolbar() {
        toolbarIncluded.toolbarNavigationIcon.isVisible = false
        toolbarIncluded.toolbarTitle.isVisible = true

        toolbarIncluded.toolbarTitle.text = "Pepul Liv"

        toolbarIncluded.ivOptions.isVisible = true
        toolbarIncluded.ivOptions.setOnClickListener { showOptions() }
    }

    private fun showOptions() {
        val username = ApplicationDependencies.getPersistentStore().username
        val menuList = listOf<MenuItem>(
            MenuItem(R.drawable.ic_logout, "Logout @$username", MENU_ID_LOGOUT, MenuItemColor.DISTINCT)
        )
        MenuSheetDialog(requireContext(), menuList) {
            when (it.id) {
                MENU_ID_LOGOUT -> {
                    handleLogout()
                }
                else -> context?.showToast("${it.title}ed")
            }
        }.also {
            it.setOnDismissListener { menuDialogReference = null }
            it.show()
            menuDialogReference = it
        }
    }

    private fun handleLogout() {
        ApplicationDependencies.getPersistentStore().logout()
        (activity as? MainActivity)?.restart()
        context?.showToast("Logged out!")
    }

    private fun gotoPublish(stream: WOWZStreamDto) {
        try {
            val publishUrl: String = stream?.sourceConnectionInfo?.run {
                "$primaryServer"
            } ?: ""

            findNavController().apply {
                val args = bundleOf(
                    PublishFragment.ARG_SDK_TYPE to SDK_TYPE_HAISHINKIT,
                    Constant.EXTRA_STREAM_URL to publishUrl,
                    Constant.EXTRA_STREAM_ID to stream.id.toString(),
                    Constant.EXTRA_STREAM_NAME to stream.sourceConnectionInfo?.streamName!!
                )
                navigate(
                    R.id.action_stream_list_to_publish_live,
                    args
                )
            }
        } catch (ignore: Exception) { }
    }

    private fun gotoWatch(streamData: StreamDto) {
        try {
            findNavController().apply {
                val args = bundleOf(
                    Constant.EXTRA_STREAM_NAME to streamData.streamName,
                    Constant.EXTRA_STREAM_ID to streamData.streamId
                )
                navigate(
                    R.id.action_stream_list_to_watch_live,
                    args
                )
            }
        } catch (ignore: Exception) { }
    }

    /*private fun dialogGetStreamName(success: (url: String) -> Any) {
        val dialog = Dialog(requireContext(), R.style.AppTheme_Dialog)
        val contentView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_stream_name, null, false)
        val edStreamHost = contentView.findViewById<TextInputEditText>(R.id.ed_streamHost)
        val edStreamName = contentView.findViewById<TextInputEditText>(R.id.ed_streamName)
        val tvUrlPreview = contentView.findViewById<TextView>(R.id.tv_urlPreview)
        val btnGo = contentView.findViewById<Button>(R.id.btn_go)
        val btnCancel = contentView.findViewById<Button>(R.id.btn_cancel)

        btnGo.setOnClickListener {
            if (isVisible) {
                val url = edStreamHost.text.toString()
                val name = edStreamName.text.toString()

                if (validateStreamData(url, name)) {
                    success.invoke(tvUrlPreview.text.toString())
                    dialog.dismiss()
                }
            }
        }

        btnCancel.setOnClickListener {
            if (isVisible && dialog.isShowing) dialog.dismiss()
        }

        val constructPreview: (TextView, String, String) -> Unit = { tv: TextView, host, name ->
            val text = "$SCHEME_RTMP$host:$DEFAULT_STREAM_PORT/$name"
            tv.text = text
        }

        edStreamHost.addTextChangedListener(
            afterTextChanged = {
                val host = edStreamHost.text.toString()
                val name = edStreamName.text.toString()

                constructPreview(tvUrlPreview, host, name)
            }
        )

        edStreamName.addTextChangedListener(
            afterTextChanged = {
                val host = edStreamHost.text.toString()
                val name = edStreamName.text.toString()

                constructPreview(tvUrlPreview, host, name)
            }
        )
        edStreamName.setText(DEFAULT_STREAM_NAME)
        edStreamHost.setText(DEFAULT_STREAM_URL)

        dialog.setContentView(contentView)
        dialog.show()

    }*/

    private fun validateStreamData(url: String, name: String): Boolean {
        if (url.isEmpty()) {
            toast("Enter stream ip/host")
            return false
        }
        if (name.isEmpty()) {
            toast("Enter stream name")
            return false
        }
        /*else if (!name.matches(Regex("[a-zA-Z\$_][/][a-zA-Z0-9\$_]*"))) {
            toast("Enter a valid name")
            return false
        }*/
        return true
    }

    private fun permissionsToast() {
        toast("Permissions required")
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT)
            .apply { setGravity(Gravity.CENTER, 0, 0) }
            .run { show() }
    }

    private fun checkPermission(): Boolean {
        if (!permissionsRequired()) return true
        ActivityCompat.requestPermissions(requireActivity(), requiredPermissions.toTypedArray(), 0)
        return false
    }

    private fun permissionsRequired(): Boolean {
        return requiredPermissions.any {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_DENIED
        }
    }

    companion object {
        private const val TAG = "StreamListFragment"

        const val SCHEME_RTMP = "rtmp://"
        const val DEFAULT_STREAM_NAME = "live/test"
        const val DEFAULT_STREAM_URL = "192.168.1."
        const val DEFAULT_STREAM_PORT = "1935"

        const val HLS_INDEX = "index.m3u8"

        const val STREAM_PLAYBACK_BASE_URL = "http://192.168.0.102:8888/live"
        const val STREAM_PUBLISH_BASE_URL = "${SCHEME_RTMP}192.168.0.102:$DEFAULT_STREAM_PORT/live/"

        private const val MENU_ID_LOGOUT    = 0
    }
}