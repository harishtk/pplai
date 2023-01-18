package com.aiavatar.app.feature.home.presentation.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.aiavatar.app.R
import com.aiavatar.app.SharedViewModel
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus
import com.aiavatar.app.databinding.FragmentAvatarStatusBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AvatarStatusFragment : Fragment() {

    private val viewModel: AvatarStatusViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

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
            uiAction = viewModel.accept
        )
        setupObservers()
    }

    private fun FragmentAvatarStatusBinding.bindState(
        uiState: StateFlow<AvatarStatusState>,
        uiAction: (AvatarStatusUiAction) -> Unit
    ) {
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
                    UploadSessionStatus.CREATING_MODEL -> {
                        description.text = "We're pouring out hearts and souls into this project, we ask for a bit more time"
                        btnCreateAvatar.isVisible = false
                        progressIndicator.isVisible = true
                        textProgressHint.isVisible = true
                        cbNotifyMe.isVisible = true
                    }
                    else -> {
                        // Noop
                    }
                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                // TODO: show loader
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