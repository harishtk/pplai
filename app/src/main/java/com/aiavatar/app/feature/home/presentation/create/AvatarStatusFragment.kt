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
import com.aiavatar.app.R
import com.aiavatar.app.SharedViewModel
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus
import com.aiavatar.app.databinding.FragmentAvatarStatusBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

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
        viewLifecycleOwner.lifecycleScope.launch {
            sessionStatusFlow.collectLatest { sessionStatus ->
                Timber.d("Session status: $sessionStatus")
                when (sessionStatus) {
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
                        // Noop
                    }
                }
            }
        }

        // TODO: combine with session status
        val avatarStatusWithFilesFlow = uiState.mapNotNull { it.avatarStatusWithFiles }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            avatarStatusWithFilesFlow.collectLatest { avatarStatusWithFiles ->
                Timber.d("Avatar status: $avatarStatusWithFiles")
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                // TODO: show loader
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
            btnCreateAvatar.setOnClickListener(null)
            uiAction(AvatarStatusUiAction.CreateModel)
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
}