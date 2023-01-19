package com.aiavatar.app.feature.home.presentation.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.R
import com.aiavatar.app.SharedViewModel
import com.aiavatar.app.commons.util.CircularProgressButtonUtil
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.commons.util.shakeNow
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus
import com.aiavatar.app.databinding.FragmentAvatarStatusBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.showToast
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.log

@AndroidEntryPoint
class AvatarStatusFragment : Fragment() {

    private val viewModel: AvatarStatusViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApplicationDependencies.getPersistentStore().currentAvatarStatusId?.let {
            viewModel.setAvatarStatusId(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_avatar_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAvatarStatusBinding.bind(view)

        // TODO: bind state
        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
        setupObservers()
    }

    private fun FragmentAvatarStatusBinding.bindState(
        uiState: StateFlow<AvatarStatusState>,
        uiAction: (AvatarStatusUiAction) -> Unit,
        uiEvent: SharedFlow<AvatarStatusUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                uiEvent.collectLatest { event ->
                    when (event) {
                        is AvatarStatusUiEvent.ShowToast -> {
                            context?.showToast(event.message.asString(requireContext()))
                        }
                    }
                }
            }
        }

        // TODO: combine with avatar status
        val sessionStatusFlow = uiState.map { it.sessionStatus }
            .distinctUntilChanged()
        /*viewLifecycleOwner.lifecycleScope.launch {
            sessionStatusFlow.collectLatest { sessionStatus ->
                Timber.d("Session status: $sessionStatus")
                when (sessionStatus) {
                    UploadSessionStatus.PARTIALLY_DONE -> {
                        *//*description.text = "Uploading photos.."
                        btnCreateAvatar.isVisible = false
                        progressIndicator.isVisible = true
                        textProgressHint.isVisible = false
                        cbNotifyMe.isVisible = false*//*
                    }
                    UploadSessionStatus.UPLOAD_COMPLETE -> {
                        *//*description.text = "Yay! Your photos for creating avatar!"
                        btnCreateAvatar.isVisible = true
                        progressIndicator.isVisible = false
                        textProgressHint.isVisible = false
                        cbNotifyMe.isVisible = false*//*
                    }
                    else -> {
                        // Noop
                    }
                }
            }
        }*/

        // TODO: combine with session status
        val avatarStatusWithFilesFlow = uiState.map { it.avatarStatusWithFiles }
            .distinctUntilChanged()
        /*viewLifecycleOwner.lifecycleScope.launch {
            avatarStatusWithFilesFlow.collectLatest { avatarStatusWithFiles ->
                Timber.d("Avatar status: $avatarStatusWithFiles")
            }
        }*/

        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                sessionStatusFlow,
                avatarStatusWithFilesFlow,
                ::Pair
            ).collectLatest { (sessionStatus, avatarStatusWithFiles) ->
                Timber.d("Avatar status: $sessionStatus $avatarStatusWithFiles")
                if (avatarStatusWithFiles != null) {
                    // Avatar status
                    when (avatarStatusWithFiles.avatarStatus.modelStatus) {
                        "training_processing" -> {
                            description.text = "We're pouring out hearts and souls into this project, we ask for a bit more time"

                            logo.isVisible = false
                            thinking.isVisible = true

                            btnCreateAvatar.isVisible = false
                            progressIndicator.isVisible = true
                            progressIndicator.isIndeterminate = true
                            textProgressHint.isVisible = true
                            cbNotifyMe.isVisible = true

                            val etaTime = getFormattedTime(avatarStatusWithFiles.avatarStatus.eta)
                            textProgressHint.text = "ETA $etaTime"
                        }
                        "avatar_processing" -> {
                            description.text = "Generating your awesome photos!"

                            logo.isVisible = false
                            thinking.isVisible = true

                            btnCreateAvatar.isVisible = false
                            progressIndicator.isVisible = true
                            progressIndicator.isIndeterminate = false
                            textProgressHint.isVisible = true
                            textProgressHint.text = "${avatarStatusWithFiles.avatarStatus.generatedAiCount}/" +
                                    "${avatarStatusWithFiles.avatarStatus.totalAiCount}"
                            val progress = (avatarStatusWithFiles.avatarStatus.generatedAiCount.toFloat() / avatarStatusWithFiles.avatarStatus.totalAiCount)
                                .coerceIn(0.0F, 1.0F)
                            progressIndicator.progress = (progress * 100).toInt()

                            cbNotifyMe.isVisible = true
                        }
                        "completed" -> {

                            logo.isVisible = true
                            thinking.isVisible = false

                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false

                            description.text = "Yay! Your avatars are ready!"
                            btnCreateAvatar.isVisible = true
                            btnCreateAvatar.text = "View Results"
                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false
                        }
                        else -> {
                            logo.isVisible = true
                            thinking.isVisible = false
                            btnCreateAvatar.isVisible = false
                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false
                        }
                    }

                } else if (sessionStatus.status <= UploadSessionStatus.UPLOAD_COMPLETE.status) {
                    when (UploadSessionStatus.fromRawValue(sessionStatus.status)) {
                        UploadSessionStatus.PARTIALLY_DONE -> {
                            description.text = "Uploading photos.."
                            btnCreateAvatar.isVisible = false
                            progressIndicator.isVisible = true
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false
                        }
                        UploadSessionStatus.UPLOAD_COMPLETE -> {
                            description.text = "Yay! Your photos for creating avatar!"
                            btnCreateAvatar.isVisible = true
                            progressIndicator.isVisible = false
                            textProgressHint.isVisible = false
                            cbNotifyMe.isVisible = false
                        }
                        else -> {
                            // Noop yet
                        }
                    }
                } else {

                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                // TODO: show loader
                /*if (btnCreateAvatar.isVisible) {
                    if (loadState.action is LoadState.Loading) {
                        btnCreateAvatar.setSpinning()
                    } else {
                        btnCreateAvatar.cancelSpinning()
                    }
                }*/
            }
        }

        val notLoadingFlow = uiState.map { it.loadState }
            .distinctUntilChanged { old, new ->
                old.refresh == new.refresh && old.action == new.action
            }
            .map { it.refresh !is LoadState.Loading && it.action !is LoadState.Loading }
        val hasErrorsFlow = uiState.map { it.exception != null }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                notLoadingFlow,
                hasErrorsFlow,
                Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorText
                    if (e != null) {
                        Timber.e(e)
                        if (btnCreateAvatar.isVisible) {
                            btnCreateAvatar.shakeNow()
                        }
                        uiErr?.let { uiText -> context?.showToast(uiText.asString(requireContext())) }
                        uiAction(AvatarStatusUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val notifyMeToggleStateFlow = uiState.map { it.toggleStateNotifyMe }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            notifyMeToggleStateFlow.collectLatest { toggleStateNotifyMe ->
                cbNotifyMe.isChecked = toggleStateNotifyMe
            }
        }

        cbNotifyMe.setOnCheckedChangeListener { _, isChecked ->
            uiAction(AvatarStatusUiAction.ToggleNotifyMe(isChecked))
        }

        val progressHintFlow = uiState.map { it.progressHint }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            progressHintFlow.collectLatest { progressHint ->
                textProgressHint.text = progressHint
            }
        }

        btnCreateAvatar.setOnClickListener {
            // btnCreateAvatar.setOnClickListener(null)
            val modelStatus = uiState.value.avatarStatusWithFiles?.avatarStatus?.modelStatus
            if (modelStatus == "completed") {
                // TODO: View results
                findNavController().apply {
                    val navOpts = NavOptions.Builder()
                        .setEnterAnim(R.anim.fade_scale_in)
                        .setExitAnim(R.anim.fade_scale_out)
                        .build()

                    navigate(R.id.avatar_result, null, navOpts)
                }
            } else {
                uiAction(AvatarStatusUiAction.CreateModel)
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.currentUploadSessionId.collectLatest { sessionId ->
                Timber.d("Session id: $sessionId")
                if (sessionId != null) {
                    viewModel.setSessionId(sessionId)
                }
            }
        }
    }

    private fun getFormattedTime(etaSeconds: Int): String {
        val millisUntilFinished = etaSeconds * 1000L
        var millisUntilFinished2 = millisUntilFinished
        var secondInMillis = millisUntilFinished / 60
        val minuteInMillis = secondInMillis * 60
        val hourInMillis = minuteInMillis * 60

        val elapsedHours: Long = TimeUnit.MILLISECONDS.toHours(millisUntilFinished2) % 60
        // millisUntilFinished2 %= hourInMillis

        val elapsedMinutes: Long = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished2) % 60
        //millisUntilFinished2 %= minuteInMillis

        val elapsedSeconds: Long = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished2) % 60

        val uiText = String.format(
            "%02d:%02d:%02d",
            elapsedHours,
            elapsedMinutes,
            elapsedSeconds
        )
        Timber.d("Timer: $uiText")
        return uiText
    }
}