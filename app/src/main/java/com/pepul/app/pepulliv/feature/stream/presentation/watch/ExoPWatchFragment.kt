package com.pepul.app.pepulliv.feature.stream.presentation.watch

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Button
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.google.gson.JsonObject
import com.pepul.app.pepulliv.Constant
import com.pepul.app.pepulliv.MainActivity
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.commons.util.AnimationUtil
import com.pepul.app.pepulliv.commons.util.Util.countDownFlow
import com.pepul.app.pepulliv.commons.util.parseViews
import com.pepul.app.pepulliv.databinding.FragmentExopWatchBinding
import com.pepul.app.pepulliv.databinding.FragmentHaishinkitPublishBinding
import com.pepul.app.pepulliv.di.ApplicationDependencies
import com.pepul.app.pepulliv.feature.onboard.presentation.login.LoginUiAction
import com.pepul.app.pepulliv.feature.stream.presentation.publish.haishinkit.HaishinKitPublishState
import com.pepul.app.pepulliv.feature.stream.presentation.publish.haishinkit.HaishinKitPublishUiAction
import com.pepul.app.pepulliv.feature.stream.presentation.util.LiveCommentsAdapter
import com.pepul.app.pepulliv.service.websocket.AppWebSocket
import com.pepul.app.pepulliv.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds


@AndroidEntryPoint
class ExoPWatchFragment : Fragment(), AppWebSocket.WebSocketEventListener {

    private var _binding: FragmentExopWatchBinding? = null
    private val binding: FragmentExopWatchBinding
        get() = _binding!!

    private val viewModel: WatchStreamViewModel by viewModels()

    private var exoPlayer: ExoPlayer? = null

    private var streamUrl: String? = null
    private var streamId: String? = null

    private var timerJob: Job? = null
    private var fakeLoaderJob: Job? = null

    private var isFirstLoad = true
    private var retryClicked = false

    private var isJoined = false

    private var retryAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            streamUrl = getString(Constant.EXTRA_STREAM_NAME)
                ?: error("No stream name")
            streamId = getString(Constant.EXTRA_STREAM_ID)
                ?: error("No stream id")
        }
        viewModel.getStreamInfo(streamId!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_exop_watch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExopWatchBinding.bind(view)

        view.findViewById<Button>(R.id.retry).setOnClickListener {
            retry()
        }

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
    }

    private fun FragmentExopWatchBinding.bindState(
        uiState: StateFlow<WatchStreamState>,
        uiAction: (WatchStreamUiAction) -> Unit,
        uiEvent: SharedFlow<WatchStreamUiEvent>,
    ) {
       viewLifecycleOwner.lifecycleScope.launchWhenStarted {
           uiEvent.collectLatest { event ->
               when (event) {
                   is WatchStreamUiEvent.ShowToast -> {
                       context?.showToast(event.message.asString(requireContext()))
                   }
               }
           }
       }

        val playbackUrlFlow = uiState.map { it.playbackUrl }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            playbackUrlFlow.collectLatest { url ->
                if (url.isNotBlank()) {
                    play(url)
                }
            }
        }

        var dotCount = 1
        fakeLoaderJob = countDownFlow(3000.milliseconds, step = 500.milliseconds)
            .onEach {
                fakeLoader.isVisible = true
                if (++dotCount == 4) dotCount = 0
                val dots = ".".repeat(dotCount)
                loadingText.text = "Starting playback${dots}"
            }
            .onCompletion {
                fakeLoader.isVisible = false
                exoPlayer?.playWhenReady = true
            }
            .catch { t -> Timber.e(t) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        val liveCommentsAdapter = LiveCommentsAdapter()

        bindLiveCommentsList(
            adapter = liveCommentsAdapter,
            uiState = uiState,
            uiAction = uiAction
        )

        bindInput(
            uiState = uiState,
            uiAction = uiAction
        )

        bindClick(uiAction = uiAction)
    }

    private fun FragmentExopWatchBinding.bindLiveCommentsList(
        adapter: LiveCommentsAdapter,
        uiState: StateFlow<WatchStreamState>,
        uiAction: (WatchStreamUiAction) -> Unit
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

    private fun FragmentExopWatchBinding.bindInput(
        uiState: StateFlow<WatchStreamState>,
        uiAction: (WatchStreamUiAction) -> Unit,
    ) {
        edMessage.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                sendMessage(uiAction)
                true
            } else {
                false
            }
        }

        edMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(uiAction)
                true
            } else {
                false
            }
        }

        edMessage.addTextChangedListener(
            afterTextChanged = { editable ->
                // uiAction(WatchStreamUiAction.TypingMessage(editable.toString().trim()))
            }
        )
    }

    private fun FragmentExopWatchBinding.sendMessage(
        uiAction: (WatchStreamUiAction.SendComment) -> Unit
    ) {
        edMessage.text.toString().trim().let {
            if (it.isNotEmpty()) {
                uiAction(WatchStreamUiAction.SendComment(it, streamId!!))
                edMessage.text = null
            } else {
                context?.showToast("Can't send empty comments")
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun FragmentExopWatchBinding.play(url: String?) {
        Timber.d("Preparing: $url")

        val (width, height) = 720 to 1280
        Glide.with(thumbnail)
            .load(MainActivity.getThumbnail(streamUrl!!))
            .thumbnail()
            .override(width, height)
            .into(thumbnail)

        val hlsUri = Uri.parse(url)

        val bandwidthMeter = DefaultBandwidthMeter.Builder(requireContext())
            .build()

        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory()

        val defaultTrackSelector = DefaultTrackSelector(requireContext(), videoTrackSelectionFactory)

        val renderersFactory = DefaultRenderersFactory(requireContext())

        val loadControl = DefaultLoadControl()

        val pb: DefaultTrackSelector.Parameters.Builder = defaultTrackSelector.buildUponParameters()
            .clearOverrides()
            .setMinVideoBitrate(20000000)
            .setMaxVideoBitrate(35000000)
            .setMaxVideoSize(720, 1280)
        defaultTrackSelector.setParameters(pb)

        val mediaDataSourceFactory =  DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(requireContext(), "pplLive"))
            .setTransferListener(bandwidthMeter)

        // Create a data source factory.
        val dataSourceFactory = DefaultHlsDataSourceFactory(mediaDataSourceFactory)

        val mediaItem = MediaItem.Builder()
            .setUri(hlsUri)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .build())
            .build()

        // Create a HLS media source pointing to a playlist uri.
        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        ExoPlayer.Builder(requireContext())
            .setTrackSelector(defaultTrackSelector)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (!isAdded) {
                        val t = IllegalStateException("$TAG Not added. Ignoring playback state change.")
                        Timber.e(t)
                        return
                    }
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            showLoading(true)
                        }
                        Player.STATE_READY -> {
                            Timber.tag("ExoPlayer.MicroMsg").d("Player err: ${exoPlayer?.playerError}")
                            showLoading(false)
                            isFirstLoad = false
                            if (exoPlayer?.playerError == null) {
                                fakeLoaderJob?.cancel(CancellationException("Content is loaded."))
                            }
                        }
                        Player.STATE_ENDED -> {
                            showLoading(false)
                            updateProgressTimer(exoPlayer?.duration!!, exoPlayer?.duration!!)
                        }
                        else -> {
                            showLoading(false)
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Timber.tag("ExoPlayer.MicroMsg").e(error)
                    val e = exoPlayer?.playerError
                    when {
                        e is ExoPlaybackException -> {
                            when  {
                                e.cause is BehindLiveWindowException ||
                                exoPlayer?.playerError?.type == ExoPlaybackException.TYPE_SOURCE -> {
                                    if (retryAttempts < 3) {
                                        context?.showToast("Failed to start playback. Retrying..")
                                        seekToDefaultPosition()
                                        prepare()
                                        playWhenReady = true
                                        retryAttempts++
                                    } else {
                                        try {
                                            context?.showToast("Failed to start playback. Source Error.")
                                            findNavController().navigateUp()
                                        } catch (ignore: Exception) {}
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    Timber.tag("ExoPlayer.MicroMsg").d("onIsPlayingChanged: $isPlaying")

                    if (isPlaying) {
                        val remainingDuration = exoPlayer?.duration!! - exoPlayer?.currentPosition!!
                        timerJob?.cancel()
                        timerJob =
                            countDownFlow(remainingDuration.coerceAtLeast(0).milliseconds)
                                .onStart {
                                    updateProgressTimer(exoPlayer?.duration!!, exoPlayer?.currentPosition!!)
                                }
                                .onEach {
                                    Timber.tag("ExoPlayer.MicroMsg").d("Remaining: ${it}ms")

                                    updateProgressTimer(exoPlayer?.currentPosition!!, exoPlayer?.duration!!)
                                }
                                .onCompletion {
                                    Timber.tag("ExoPlayer.MicroMsg")
                                        .d("Video paused or stopped")
                                }
                                .catch { t ->
                                    Timber.tag("ExoPlayer.MicroMsg").e(t)
                                    // throw t
                                }
                                .launchIn(viewLifecycleOwner.lifecycleScope)

                        if (!isJoined) {
                            val payload = JsonObject().apply {
                                addProperty("userId", ApplicationDependencies.getPersistentStore().userId)
                                addProperty("streamId", streamId)
                            }
                            ApplicationDependencies.getAppWebSocket().messageBroker.joinStream(payload.toString())
                            isJoined = true
                        }
                        watchCountContainer.isVisible = true
                    } else {
                        timerJob?.cancel(CancellationException("Video paused or stopped"))
                    }
                }


            })
            //setMediaItem(mediaItem)
            setMediaSource(hlsMediaSource)
            // volume = 0f
            playWhenReady = retryClicked
            prepare()
            retryClicked = false
        }.also { exoPlayer = it }

        binding.apply {
            playerView.apply {
                useController = false
                keepScreenOn = true
                player = exoPlayer
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShowRewindButton(false)
                setShowFastForwardButton(false)
                setShowPreviousButton(false)
                setShowNextButton(false)
            }
        }

        val gestureDetector = GestureDetector(requireContext(), PlayerGestureListener())
        gestureFrame.setOnTouchListener { _, event ->
            Timber.d("onTouchEvent: $event")
            return@setOnTouchListener gestureDetector.onTouchEvent(event)
        }

        likeButton.setOnClickListener {
            sendLike()
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.bubble_feedback)
            it.apply {
                startAnimation(anim)
            }
        }
    }

    private fun FragmentExopWatchBinding.bindClick(
        uiAction: (WatchStreamUiAction) -> Unit
    ) {
        sendButton.setOnClickListener { sendMessage(uiAction) }
    }

    private fun updateProgressTimer(currentPosition: Long, duration: Long) = with(binding) {
        val remainingDuration = exoPlayer?.duration!! - exoPlayer?.currentPosition!!
        // val progress = abs((currentPosition.toFloat() / duration.toFloat()) * 100.0 - 100.0) // reversed
        val progress = (currentPosition.toFloat() / duration.toFloat()) * 100.0
        val previous = timedProgressBar.progress
        val progressAnimator = ValueAnimator.ofInt(previous, progress.toInt()).apply {
            this.duration = 1000L
            interpolator = LinearInterpolator()
            this.addUpdateListener {
                timedProgressBar.progress = it.animatedValue as Int
            }
            start()
        }
        this.tvRemainingDuration.text = (remainingDuration / 1000).toString()
        timedProgressBar.isVisible = remainingDuration > 0
        // Property.of(CircularProgressIndicator::class.java, Int::class.java, "progress")
        // timedProgressBar.progress = progress.toInt()
        Timber.d("Progress:  $currentPosition $duration $progress")
    }

    private fun showLoading(show: Boolean) = with(binding) {
        Timber.tag("ExoPlayer.MicroMsg").d("Loading: $show")
        thumbnail.isVisible = show && isFirstLoad
        val isFakeLoader = (fakeLoaderJob?.isActive == true)
        if (show && !isFakeLoader) {
            loadingBar.show()
        } else {
            loadingBar.hide()
        }
    }

    private fun retry() {
        exoPlayer?.apply {
            stop()
            release()
        }
        if (binding != null) {
            binding.play(viewModel.uiState.value.playbackUrl)
            retryClicked = true
        }
    }

    private fun sendLike(): Boolean {
        if (isJoined) {
            val payload = JsonObject().apply {
                addProperty("userId", ApplicationDependencies.getPersistentStore().userId)
                addProperty("streamId", streamId)
            }
            ApplicationDependencies.getAppWebSocket().messageBroker
                .likeStream(payload.toString())
            return true
        }
        return false
    }

    private fun updateWatchCount(subscriberCount: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            binding.watchCount.text = "${parseViews(subscriberCount)} Watching"
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

    override fun onPause() {
        if (isJoined) {
            val payload = JsonObject().apply {
                addProperty("userId", ApplicationDependencies.getPersistentStore().userId)
                addProperty("streamId", streamId)
            }
            ApplicationDependencies.getAppWebSocket().messageBroker.leaveStream(payload.toString())
            isJoined = false
        }
        ApplicationDependencies.getAppWebSocket().unregisterListener(this)
        super.onPause()
        exoPlayer?.apply {
            playWhenReady = false
            playbackState
        }
    }

    override fun onResume() {
        super.onResume()
        ApplicationDependencies.getAppWebSocket().registerListener(this)
        exoPlayer?.apply {
            playWhenReady = true && !isFirstLoad
            playbackState
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        _binding = null
    }

    inner class PlayerGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDoubleTap(e: MotionEvent): Boolean {
            sendLike()
            return super.onDoubleTap(e)
        }
    }

    companion object {
        val TAG = ExoPWatchFragment::class.java.simpleName
    }
}