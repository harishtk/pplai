package com.pepul.app.pepulliv.feature.stream.presentation.publish

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pepul.app.pepulliv.Constant
import com.pepul.app.pepulliv.Constant.STREAM_STATE_NEW
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STARTED
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STARTING
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STOPPED
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.commons.util.HapticUtil
import com.pepul.app.pepulliv.databinding.FragmentDefaultPublishBinding
import com.pepul.app.pepulliv.showToast
import com.takusemba.rtmppublisher.Publisher
import com.takusemba.rtmppublisher.PublisherListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * A Default RTMP publisher
 */
@AndroidEntryPoint
class DefaultPublishFragment : Fragment(), PublisherListener {

    private val viewModel: DefaultPublishViewModel by viewModels()

    private lateinit var publisher: Publisher

    private lateinit var publishButton: Button
    private lateinit var label: TextView

    private val handler = Handler()
    private var thread: Thread? = null
    private var isCounting = false

    private var streamUrl: String? = null
    private var streamId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            streamUrl = getString(Constant.EXTRA_STREAM_URL)
                ?: error("No stream url")
            streamId = getString(Constant.EXTRA_STREAM_ID)
                ?: error("No stream id")
        }

        Timber.d("$streamUrl <-- Preparing to publish..")
        if (savedInstanceState == null) {
            viewModel.startPublish(streamId!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_default_publish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentDefaultPublishBinding.bind(view)

        publishButton = binding.togglePublish
        label = binding.liveLabel

        if (streamUrl.isNullOrEmpty()) {
            Toast.makeText(requireContext(), R.string.error_empty_url, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        } else {
            publisher = Publisher.Builder(requireActivity() as AppCompatActivity, lifecycle)
                .setGlView(binding.surfaceView)
                .setUrl(streamUrl!!)
                .setSize(Publisher.Builder.DEFAULT_WIDTH, Publisher.Builder.DEFAULT_HEIGHT)
                .setAudioBitrate(AUDIO_BITRATE)
                .setVideoBitrate(VIDEO_BITRATE)
                .setCameraMode(Publisher.Builder.DEFAULT_MODE)
                .setListener(this)
                .build()

            binding.togglePublish.setOnClickListener {
                if (viewModel.uiState.value.streamState != STREAM_STATE_STARTED) {
                    context?.showToast("Please wait while we are preparing your stream.")
                    HapticUtil.createError(requireContext())
                } else {
                    if (publisher.isPublishing) {
                        publisher.stopPublishing()
                    } else {
                        publisher.startPublishing()
                    }
                    HapticUtil.createOneShot(requireContext())
                }
            }

            binding.toggleCamera.setOnClickListener {
                publisher.switchCamera()
            }
        }

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
    }

    private fun FragmentDefaultPublishBinding.bindState(
        uiState: StateFlow<DefaultPublishState>,
        uiAction: (DefaultPublishUiAction) -> Unit,
        uiEvent: SharedFlow<DefaultPublishUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            uiEvent.collectLatest { event ->
                when (event) {
                    is DefaultPublishUiEvent.ShowToast -> {
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
                        streamStatusText.text = "Your stream is starting."
                    }
                    STREAM_STATE_STARTED -> {
                        // publisher.startPublishing()
                        streamStatusText.isVisible = true
                        streamStatusText.text = "Ready to go live."
                    }
                    STREAM_STATE_STOPPED -> {
                        streamStatusText.isVisible = true
                        streamStatusText.text = "Your stream is ended."
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

    }

    private fun updateControls() {
        publishButton.text = getString(if (publisher.isPublishing) R.string.stop_publishing else R.string.start_publishing)
    }

    private fun startCounting() {
        isCounting = true
        label.text = getString(R.string.publishing_label, 0L.format(), 0L.format())
        label.visibility = View.VISIBLE
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
                        label.text = getString(R.string.publishing_label, min.format(), second.format())
                    }
                }
            }
        }
        thread?.start()
    }

    private fun stopCounting() {
        val duration = label.text
        isCounting = false
        label.text = ""
        label.visibility = View.GONE
        thread?.interrupt()
        Log.d(TAG, "Stream: ended in $duration")
    }

    private fun Long.format(): String {
        return String.format("%02d", this)
    }

    override fun onResume() {
        super.onResume()
        updateControls()
    }

    override fun onStarted() {
        Toast.makeText(requireContext(), R.string.started_publishing, Toast.LENGTH_SHORT)
            .apply { setGravity(Gravity.CENTER, 0, 0) }
            .run { show() }
        updateControls()
        startCounting()
    }

    override fun onStopped() {
        Toast.makeText(requireContext(), R.string.stopped_publishing, Toast.LENGTH_SHORT)
            .apply { setGravity(Gravity.CENTER, 0, 0) }
            .run { show() }
        updateControls()
        stopCounting()
        viewModel.stopPublish(streamId!!)
    }

    override fun onDisconnected() {
        Toast.makeText(requireContext(), R.string.disconnected_publishing, Toast.LENGTH_SHORT)
            .apply { setGravity(Gravity.CENTER, 0, 0) }
            .run { show() }
        updateControls()
        stopCounting()
        viewModel.stopPublish(streamId!!)
    }

    override fun onFailedToConnect() {
        Toast.makeText(requireContext(), R.string.failed_publishing, Toast.LENGTH_SHORT)
            .apply { setGravity(Gravity.CENTER, 0, 0) }
            .run { show() }
        updateControls()
        stopCounting()
        if (publisher.isPublishing) {
            viewModel.stopPublish(streamId!!)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_STREAM_STATE, viewModel.uiState.value.streamState)
    }

    override fun onDestroyView() {
        viewModel.stopPublish(streamId!!)
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "PublishFragment"

        const val AUDIO_BITRATE = 6400
        const val VIDEO_BITRATE = 100000

        const val EXTRA_STREAM_STATE = "com.pepul.com.pepullive.extras.STREAM_STATE"
    }
}