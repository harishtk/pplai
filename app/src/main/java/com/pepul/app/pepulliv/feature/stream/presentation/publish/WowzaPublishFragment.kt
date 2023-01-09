package com.pepul.app.pepulliv.feature.stream.presentation.publish

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.pepul.app.pepulliv.Constant
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.databinding.FragmentWowzaPublishBinding
import com.pepul.app.pepulliv.showToast
import com.wowza.gocoder.sdk.api.WowzaGoCoder
import com.wowza.gocoder.sdk.api.broadcast.WOWZBroadcast
import com.wowza.gocoder.sdk.api.broadcast.WOWZBroadcastConfig
import com.wowza.gocoder.sdk.api.configuration.WOWZMediaConfig
import com.wowza.gocoder.sdk.api.devices.WOWZAudioDevice
import com.wowza.gocoder.sdk.api.devices.WOWZCamera
import com.wowza.gocoder.sdk.api.devices.WOWZCameraView
import com.wowza.gocoder.sdk.api.errors.WOWZError
import com.wowza.gocoder.sdk.api.geometry.WOWZSize
import com.wowza.gocoder.sdk.api.render.WOWZRenderAPI.VideoFrameListener
import com.wowza.gocoder.sdk.api.status.WOWZState
import com.wowza.gocoder.sdk.api.status.WOWZStatus
import com.wowza.gocoder.sdk.api.status.WOWZStatusCallback
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class WowzaPublishFragment : Fragment(), WOWZStatusCallback, WOWZCameraView.PreviewStatusListener {

    private var _binding: FragmentWowzaPublishBinding? = null
    private val binding: FragmentWowzaPublishBinding
        get() = _binding!!

    private var streamUrl: String? = null
    private var streamId: String? = null

    var sGoCoderSDK: WowzaGoCoder? = null
    private var goCoderBroadcaster: WOWZBroadcast? = null
    private var goCoderBroadcastConfig: WOWZBroadcastConfig? = null
    private val videoFrameListener: VideoFrameListener? = null
    private val mGrabFrame = AtomicBoolean(false)
    private val mSavingFrame = AtomicBoolean(false)
    private val handler = Handler()
    private var mWZAudioDevice: WOWZAudioDevice? = null

    private var isStreamStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            streamUrl = getString(Constant.EXTRA_STREAM_URL)
                ?: error("No stream url")
            streamId = getString(Constant.EXTRA_STREAM_ID)
                ?: error("No stream id")
        }

        Timber.d("$streamUrl <-- Preparing to publish..")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_wowza_publish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWowzaPublishBinding.bind(view)

        sGoCoderSDK = WowzaGoCoder.init(context?.applicationContext!!, "")

        if (sGoCoderSDK == null) {
            // If initialization failed, retrieve the last error and display it
            val goCoderInitError = WowzaGoCoder.getLastError()
            context?.showToast("GoCoder SDK error: ${goCoderInitError.errorDescription}")
            return
        }

        binding.initializeLay.visibility = View.VISIBLE

    }

    private fun initGoCoderBroadCast() {
        goCoderBroadcaster = WOWZBroadcast()
        mWZAudioDevice = WOWZAudioDevice()
        // Create a configuration instance for the broadcaster
        goCoderBroadcastConfig = WOWZBroadcastConfig(WOWZMediaConfig.FRAME_SIZE_352x288).apply {
            // Set the connection properties for the target Wowza Streaming Engine server or Wowza Streaming Cloud live stream
            /*hostAddress = AdminData.streamDetails.getStreamHost()
            portNumber = AdminData.streamDetails.getStreamPort()
            applicationName = AdminData.streamDetails.getStreamApplicationName()
            streamName = streamName
            username = AdminData.streamDetails.getStreamUserName()
            password = AdminData.streamDetails.getStreamPassword()*/
            // Designate the camera preview as the video broadcaster
            videoBroadcaster = binding.cameraPreview
            // Designate the audio device as the audio broadcaster
            audioBroadcaster = mWZAudioDevice
            isVideoEnabled = true
    //        goCoderBroadcastConfig.setVideoBitRate(1500);
    //        goCoderBroadcastConfig.setVideoFramerate(24);
        }

    }

    private fun startBroadCast() {
        // Ensure the minimum set of configuration settings have been specified necessary to
        // initiate a broadcast streaming session
        val configValidationError = goCoderBroadcastConfig!!.validateForBroadcast()
        if (configValidationError != null) {
            Log.i(TAG,
                "startBroadCast: " + configValidationError.errorDescription)
        } else if (goCoderBroadcaster!!.status.isRunning) {
            // Stop the broadcast that is currently running
            goCoderBroadcaster!!.endBroadcast(this)
        } else {
            // Start streaming
            goCoderBroadcaster!!.startBroadcast(goCoderBroadcastConfig, this)
        }
    }

    private fun stopBroadCast() {
        if (goCoderBroadcaster != null && goCoderBroadcaster!!.status.isRunning) {
            // Stop the broadcast that is currently running
            goCoderBroadcaster!!.endBroadcast(this)
        }
    }

    override fun onWZCameraPreviewStarted(p0: WOWZCamera?, p1: WOWZSize?, p2: Int) {
        startBroadCast()
    }

    override fun onWZCameraPreviewStopped(p0: Int) {
        // Noop.
    }

    override fun onWZCameraPreviewError(p0: WOWZCamera?, p1: WOWZError?) {
        Log.e(TAG,
            "onWZCameraPreviewError: " + Gson().toJson(p1))
        activity?.finish()
    }

    override fun onWZStatus(goCoderStatus: WOWZStatus) {
        // A successful status transition has been reported by the GoCoder SDK

        // A successful status transition has been reported by the GoCoder SDK
        val isStreaming = goCoderBroadcaster!!.status.isRunning
        Log.i(TAG,
            "isStreaming: $isStreaming")
        Log.i(TAG,
            "onWZStatus: " + Gson().toJson(goCoderStatus))
        val statusMessage = StringBuffer("Broadcast status: ")

        when (goCoderStatus.getState()) {
            WOWZState.STARTING -> statusMessage.append("Broadcast initialization")
            WOWZState.READY -> statusMessage.append("Ready to begin streaming")
            WOWZState.RUNNING -> {
                if (!isStreamStarted) {
                    isStreamStarted = true
                    // register our newly created video frame listener wth the camera preview display view
                    // mWZCameraView.registerFrameListener(videoFrameListener)
                    // Display the status message using the U/I thread
                   /* Handler(Looper.getMainLooper()).post {
                        loadingLay.setVisibility(View.GONE)
                        initializeLay.setVisibility(View.GONE)
                        hideProgress()
                    }*/
                }
                statusMessage.append("Streaming is active")
            }
            WOWZState.STOPPING -> {
                statusMessage.append("Broadcast shutting down")

            }
            WOWZState.IDLE -> statusMessage.append("The broadcast is stopped")
            WOWZState.ERROR -> {
                statusMessage.append("The broadcast is ERROR")
            }
            else -> return
        }

    }

    override fun onWZError(p0: WOWZStatus) {
        Timber.e(p0.getLastError().exception, "WOWZ Error")
    }

    override fun onResume() {
        super.onResume()

        // Start the camera preview display
        if (binding.cameraPreview != null) {
            if (binding.cameraPreview.isPreviewPaused) {
                binding.cameraPreview.onResume()
            } else {
                binding.cameraPreview.startPreview(this)
            }
        }
    }

    companion object {
        private const val TAG = "WowzaPublishFragment"
    }
}