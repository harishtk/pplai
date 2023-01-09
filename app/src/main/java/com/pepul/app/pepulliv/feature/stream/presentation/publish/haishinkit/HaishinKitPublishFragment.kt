package com.pepul.app.pepulliv.feature.stream.presentation.publish.haishinkit

import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.haishinkit.event.Event
import com.haishinkit.event.EventUtils
import com.haishinkit.event.IEventListener
import com.haishinkit.graphics.effect.DefaultVideoEffect
import com.haishinkit.graphics.effect.MonochromeVideoEffect
import com.haishinkit.media.AudioRecordSource
import com.haishinkit.media.Camera2Source
import com.haishinkit.rtmp.RtmpConnection
import com.haishinkit.rtmp.RtmpStream
import com.haishinkit.view.NetStreamDrawable
import com.pepul.app.pepulliv.Constant
import com.pepul.app.pepulliv.Constant.STREAM_STATE_NEW
import com.pepul.app.pepulliv.Constant.STREAM_STATE_PUBLISHING
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STARTED
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STARTING
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STOPPED
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.commons.util.HapticUtil
import com.pepul.app.pepulliv.commons.util.StorageUtil
import com.pepul.app.pepulliv.commons.util.StorageUtil.FIRST_THUMBNAIL_FILENAME
import com.pepul.app.pepulliv.commons.util.StorageUtil.THUMB_PREFIX
import com.pepul.app.pepulliv.commons.util.parseViews
import com.pepul.app.pepulliv.databinding.FragmentHaishinkitPublishBinding
import com.pepul.app.pepulliv.di.ApplicationDependencies
import com.pepul.app.pepulliv.feature.stream.presentation.publish.DefaultPublishFragment
import com.pepul.app.pepulliv.feature.stream.presentation.util.LiveCommentsAdapter
import com.pepul.app.pepulliv.pad
import com.pepul.app.pepulliv.service.websocket.AppWebSocket
import com.pepul.app.pepulliv.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class HaishinKitPublishFragment : Fragment(), IEventListener, AppWebSocket.WebSocketEventListener {

    private var _binding: FragmentHaishinkitPublishBinding? = null
    private val binding: FragmentHaishinkitPublishBinding
        get() = _binding!!

    private val viewModel: HaishinKitPubViewModel by viewModels()

    private lateinit var connection:    RtmpConnection
    private lateinit var stream:        RtmpStream
    private lateinit var cameraView:    NetStreamDrawable
    private lateinit var cameraSource:  Camera2Source

    // Timer related
    private val handler = Handler()
    private var thread: Thread? = null
    private var isCounting = false

    private lateinit var streamUrl: String
    private lateinit var streamId: String
    private lateinit var streamName: String

    private var isReadyToStream = false
    private var onBackPressed = false
    private var isRecording = false

    private var thumbnailGenJob: Job? = null
    private var keepAliveJob: Job? = null
    @Volatile private var streamStartTime: Long = 0

    val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val streamState = viewModel.uiState.value.streamState
            if (streamState != STREAM_STATE_STOPPED) {
                AlertDialog.Builder(requireContext())
                    .setMessage("Stop stream?")
                    .setPositiveButton("Yes") { _, _ ->
                        onBackPressed = true
                        connection.close()
                        viewModel.stopPublish(streamId)
                        stopThumbnailGenerator("Stream ended")
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.cancel() }
                    .show()
            } else {
                try { findNavController().navigateUp() }
                catch (ignore: Exception) {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connection  = RtmpConnection()
        stream      = RtmpStream(connection)
        stream.currentFPS
        stream.attachAudio(AudioRecordSource(requireContext()))

        cameraSource = Camera2Source(requireContext())
        stream.attachVideo(cameraSource)

        connection.addEventListener(Event.RTMP_STATUS, this)

        arguments?.apply {
            streamUrl = getString(Constant.EXTRA_STREAM_URL)
                ?: error("No stream url")
            streamId = getString(Constant.EXTRA_STREAM_ID)
                ?: error("No stream id")
            streamName = getString(Constant.EXTRA_STREAM_NAME)
                ?: error("No stream name")

            Preference.shared.rtmpURL = streamUrl
            Preference.shared.streamName = streamName
            viewModel.startPublish(streamId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_haishinkit_publish, container, false)

        /*val save = v.findViewById<Button>(R.id.save_button)
        save.setOnClickListener {
            cameraView.readPixels {
                val bitmap = it ?: return@readPixels

                val file = File(requireContext().externalCacheDir, "share_temp.jpeg")
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.flush()
                }

                val fileUri: Uri? = try {
                    FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().packageName + ".fileprovider",
                        file
                    )
                } catch (e: IllegalArgumentException) {
                    null
                }

                fileUri ?: run {
                    return@readPixels
                }

                val builder = this@HaishinKitPublishFragment.activity?.let { it1 ->
                    ShareCompat.IntentBuilder.from(
                        it1
                    )
                }?.apply {
                    addStream(fileUri)
                    setType(requireContext().contentResolver.getType(fileUri))
                }?.createChooserIntent()?.apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    resolveActivity(requireContext().packageManager)?.also {
                        startActivity(this)
                    }
                }
            }
        }*/

        val filter = v.findViewById<Button>(R.id.filter_button)
        filter.setOnClickListener {
            if (filter.text == "Normal") {
                stream.videoEffect = MonochromeVideoEffect()
                filter.text = "Mono"
            } else {
                stream.videoEffect = DefaultVideoEffect.shared
                filter.text = "Normal"
            }
        }

        cameraView = if (Preference.useSurfaceView) {
            v.findViewById(R.id.surface_view)
        } else {
            v.findViewById(R.id.texture_view)
        }
        cameraView.attachStream(stream)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHaishinkitPublishBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        setupBackPressedCallback()
    }

    private fun FragmentHaishinkitPublishBinding.bindState(
        uiState: StateFlow<HaishinKitPublishState>,
        uiAction: (HaishinKitPublishUiAction) -> Unit,
        uiEvent: SharedFlow<HaishinKitPublishUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            uiEvent.collectLatest { event ->
                when (event) {
                    is HaishinKitPublishUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                }
            }
        }

        val streamStateFlow = uiState.map { it.streamState }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            streamStateFlow.collectLatest { streamState ->
                when (streamState) {
                    STREAM_STATE_STARTING -> {
                        streamStatusText.isVisible = true
                        streamStatusText.text = "Your stream is starting.."
                    }
                    STREAM_STATE_STARTED -> {
                        isReadyToStream = true
                        streamStatusText.isVisible = true
                        streamStatusText.text = "Ready to go live."
                        ApplicationDependencies.getAppWebSocket().messageBroker.apply {
                            val payload = JsonObject().apply {
                                addProperty("userId", ApplicationDependencies.getPersistentStore().userId)
                                addProperty("streamId", streamId)
                            }
                            publishStream(payload.toString())
                        }
                        scheduleKeepAlive(streamId)
                        /*AnimationUtils.loadAnimation(requireContext(), R.anim.bubble_feedback).apply {
                            repeatMode = Animation.RESTART
                            repeatCount = Animation.INFINITE
                            liveRecordButton.startAnimation(this)
                        }*/
                    }
                    STREAM_STATE_PUBLISHING -> {
                        streamStatusText.isVisible = false
                    }
                    STREAM_STATE_STOPPED -> {
                        isReadyToStream = false
                        stopCounting()
                        streamStatusText.isVisible = true
                        streamStatusText.text = "Your stream is ended."
                        context?.showToast("Your stream is ended.")
                        ApplicationDependencies.getAppWebSocket().messageBroker.apply {
                            val streamDuration = (System.currentTimeMillis() - streamStartTime).coerceAtLeast(0)
                            val payload = JsonObject().apply {
                                addProperty("userId", ApplicationDependencies.getPersistentStore().userId)
                                addProperty("streamId", streamId)
                                addProperty("streamDuration", streamDuration)
                            }
                            stopStream(payload.toString())
                        }
                        keepAliveJob?.cancel(CancellationException("Stream ended"))
                        if (onBackPressed) {
                            onBackPressed = false
                            try { findNavController().navigateUp() }
                            catch (ignore: Exception) {}
                        }
                        streamStartTime = 0
                    }
                    STREAM_STATE_NEW -> {
                        streamStatusText.isVisible = true
                        streamStatusText.text = "Connecting.."
                    }
                    else -> {
                        streamStatusText.isVisible = false
                    }
                }
            }
        }

        val liveRecordButtonStateFlow = uiState.map { it.recordButtonState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            liveRecordButtonStateFlow.collectLatest { liveRecordButtonState ->
                when (liveRecordButtonState) {
                    LiveRecordButtonState.RECORDING -> {
                        liveRecordButton.clearAnimation()
                        liveRecordButton.isEnabled = true
                        centerImage.setImageResource(R.drawable.squared_fill)
                    }
                    LiveRecordButtonState.PREPARING -> {
                        liveRecordButton.isEnabled = true
                    }
                    LiveRecordButtonState.IDLE -> {
                        liveRecordButton.isEnabled = true
                        centerImage.setImageResource(R.drawable.rounded_fill)
                    }
                }
            }
        }

        val liveCommentsAdapter = LiveCommentsAdapter()

        bindLiveCommentsList(
            adapter = liveCommentsAdapter,
            uiState = uiState,
            uiAction = uiAction
        )

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )
    }

    private fun FragmentHaishinkitPublishBinding.bindLiveCommentsList(
        adapter: LiveCommentsAdapter,
        uiState: StateFlow<HaishinKitPublishState>,
        uiAction: (HaishinKitPublishUiAction) -> Unit
    ) {
        liveCommentsContainer.isVisible = true
        liveCommentsList.adapter = adapter

        val liveCommentsFlow = uiState.map { it.liveComment }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            liveCommentsFlow.collectLatest { commentList ->
                Timber.d("Comments adapter: $commentList")
                adapter.submitList(commentList)
            }
        }
    }

    private fun FragmentHaishinkitPublishBinding.bindClick(
        uiState: StateFlow<HaishinKitPublishState>,
        uiAction: (HaishinKitPublishUiAction) -> Unit
    ) {
        liveRecordButton.setOnClickListener {
            /*if (button.text == "Publish") {
                if (viewModel.uiState.value.streamState != STREAM_STATE_STARTED) {
                    context?.showToast("Please wait while we are preparing your stream.")
                    if (viewModel.uiState.value.streamState != STREAM_STATE_STARTING) {
                        viewModel.startPublish(streamId)
                    }
                    HapticUtil.createError(requireContext())
                } else {
                    connection.connect(Preference.shared.rtmpURL)
                    button.text = "Stop"
                }
            } else {
                connection.close()
                button.text = "Publish"
                viewModel.stopPublish(streamId)
                stopThumbnailGenerator("Stream ended")
            }
            HapticUtil.createOneShot(requireContext())*/
            val streamState = uiState.value.streamState
            if (streamState == STREAM_STATE_PUBLISHING) {
                if (!connection.isConnected) { connection.close() }
                onBackPressed = true /* This kicks out the user off the screen */
                // button.text = "Publish"
                viewModel.stopPublish(streamId)
                stopThumbnailGenerator("Stream ended")
            } else if (streamState == STREAM_STATE_STARTING) {
                context?.showToast("Please wait while we are preparing your stream.")
                HapticUtil.createError(requireContext())
            } else if (streamState == STREAM_STATE_STARTED) {
                connection.connect(Preference.shared.rtmpURL)
            } else {
                if (streamState == STREAM_STATE_STOPPED) {
                    context?.showToast("Please wait while we are preparing your stream.")
                    if (viewModel.uiState.value.streamState != STREAM_STATE_STARTING) {
                        viewModel.startPublish(streamId)
                    }
                    HapticUtil.createError(requireContext())
                } else {
                    viewModel.startPublish(streamId)
                }
            }
        }

        ivSwitch.setOnClickListener {
            cameraSource.switchCamera()
        }
    }

    private fun startThumbnailGenerator() {
        thumbnailGenJob?.cancel(CancellationException("New request"))
        thumbnailGenJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                cameraView.readPixels {
                    if (it != null) {
                        val elapsed = if (streamStartTime > 0) {
                            (System.currentTimeMillis() - streamStartTime)
                        } else {
                            0
                        }
                        val fileName = THUMB_PREFIX + String.format("%02d", (elapsed / 1000))
                        val thumb = StorageUtil.saveThumbnail(requireContext(), it, streamId, fileName) ?: return@readPixels
                        Log.d(TAG, "startThumbnailGenerator: saved to ${thumb?.absolutePath}")
                        viewModel.uploadThumbnail(
                            file = thumb,
                            streamName = streamId,
                        )
                        if (fileName == FIRST_THUMBNAIL_FILENAME) {
                            val fileName05 = fileName.replace("00", "05")
                            viewModel.uploadThumbnail(
                                file = thumb,
                                streamName = streamId,
                                fileName = fileName05
                            )
                        }
                    }
                }
                delay(THUMBNAIL_GENERATOR_DELAY)
            }
        }
    }

    private fun stopThumbnailGenerator(reason: String) {
        thumbnailGenJob?.cancel(CancellationException(reason))
    }

    private fun updateWatchCount(subscriberCount: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            binding.watchCount.text = parseViews(subscriberCount)
        }
    }

    /* TODO: Move counting to View Model */
    private fun startCounting() = with(binding) {
        isCounting = true
        liveLabel.text = getString(R.string.publishing_label, 0L.pad(), 0L.pad())
        liveLabelContainer.visibility = View.VISIBLE
        watchCount.isVisible = true
        updateWatchCount(0)
        val startedAt = System.currentTimeMillis()
        var updatedAt = System.currentTimeMillis()
        thread = Thread {
            while (isCounting) {
                if (System.currentTimeMillis() - updatedAt > 1000) {
                    updatedAt = System.currentTimeMillis()
                    handler.post {
                        val diff = System.currentTimeMillis() - startedAt
                        val second = diff / 1000 % 60
                        val min = diff / 1000 / 60
                        liveLabel.text = getString(R.string.publishing_label, min.pad(), second.pad())
                    }
                }
            }
        }
        thread?.start()
    }

    private fun stopCounting() = with(binding) {
        val duration = liveLabel.text
        isCounting = false
        liveLabel.text = ""
        liveLabelContainer.visibility = View.GONE
        watchCount.isVisible = false
        thread?.interrupt()
        Log.d(TAG, "Stream: ended in $duration")
    }

    private fun setupBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun scheduleKeepAlive(streamId: String) {
        keepAliveJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                if (!isReadyToStream) break
                delay(TimeUnit.MINUTES.toMillis(1))
                if (isReadyToStream) {
                    val payload = JsonObject().apply {
                        addProperty("userId", ApplicationDependencies.getPersistentStore().userId)
                        addProperty("streamId", streamId)
                    }
                    ApplicationDependencies.getAppWebSocket().messageBroker
                        .sendKeepAlive(payload.toString())
                }
            }
        }
    }

    override fun onStreamStatus(payload: String) {
        // Noop
    }

    override fun onSubscriberJoined(payload: String) {
        try {
            val payloadJson = JSONObject(payload)
            val count = payloadJson.getInt("subscriberCount")
            updateWatchCount(count)
            viewModel.subscriberJoined(payloadJson)
        } catch (e: JSONException) {
            Timber.e(e)
        }
    }

    override fun onSubscriberLeft(payload: String) {
        try {
            val payloadJson = JSONObject(payload)
            val count = payloadJson.getInt("subscriberCount")
            updateWatchCount(count)
            viewModel.subscriberLeft(payloadJson)
        } catch (e: JSONException) {
            Timber.e(e)
        }
    }

    override fun onStreamLiked(payload: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            binding.heartLayout.addHeart(Color.RED)
        }
    }

    override fun onStreamComment(payload: String) {
        try {
            val payloadJson = JSONObject(payload)
            viewModel.commentReceived(payloadJson)
        } catch (e: JSONException) {
            Timber.e(e)
        }
    }

    override fun onResume() {
        super.onResume()
        ApplicationDependencies.getAppWebSocket().registerListener(this)
        cameraSource.open(CameraCharacteristics.LENS_FACING_FRONT)
    }

    override fun onPause() {
        ApplicationDependencies.getAppWebSocket().unregisterListener(this)
        cameraSource.close()
        super.onPause()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.i("$TAG#onAttach", "called")
    }

    override fun onDetach() {
        super.onDetach()
        Log.i("$TAG#onDetach", "called")
    }

    override fun onDestroyView() {
        Preference.clear()
        // No guarantee that this would succeed
        viewModel.stopPublish(streamId)
        stopThumbnailGenerator("Stream ended")
        super.onDestroyView()
    }

    override fun handleEvent(event: Event) {
        Log.i("$TAG#handleEvent", event.data.toString())
        val data = EventUtils.toMap(event)
        val code = data["code"].toString()
        if (code == RtmpConnection.Code.CONNECT_SUCCESS.rawValue) {
            streamStartTime = System.currentTimeMillis()
            stream.publish(Preference.shared.streamName)
            viewModel.startedPublishing()
            startCounting()
            startThumbnailGenerator()
        } else if (code == RtmpConnection.Code.CONNECT_CLOSED.rawValue) {   
            viewModel.stopPublish(streamId)
            stopThumbnailGenerator("Stream ended")
        }
    }

    companion object {

        fun newInstance(): HaishinKitPublishFragment {
            return HaishinKitPublishFragment()
        }

        private val TAG = HaishinKitPublishFragment::class.java.simpleName

        private const val THUMBNAIL_GENERATOR_DELAY = 5000L
    }

}