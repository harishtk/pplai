package com.aiavatar.app.feature.home.presentation.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.aiavatar.app.*
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.commons.util.shakeNow
import com.aiavatar.app.databinding.FragmentSubscriptionSuccessBinding
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SubscriptionSuccessFragment : Fragment() {

    private val viewModel: SubscriptionSuccessViewModel by viewModels()

    private lateinit var planId: String
    private var pagePresentedAt: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        planId = arguments?.getString(Constant.ARG_PLAN_ID, "") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_subscription_success, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSubscriptionSuccessBinding.bind(view)

        // TODO: bind state
        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
        pagePresentedAt = System.currentTimeMillis()
        // viewModel.generateAvatarRequest(planId)
    }

    private fun FragmentSubscriptionSuccessBinding.bindState(
        uiState: StateFlow<SubscriptionSuccessState>,
        uiAction: (SubscriptionSuccessUiAction) -> Unit,
        uiEvent: SharedFlow<SubscriptionSuccessUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is SubscriptionSuccessUiEvent.ShowToast -> {
                       context?.showToast(event.message.asString(requireContext()))
                    }
                    is SubscriptionSuccessUiEvent.NextScreen -> {
                        nextButton.setOnClickListener(null)
                        val delta = System.currentTimeMillis() - pagePresentedAt
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay((UI_PRESENTATION_TIME - delta).coerceAtLeast(0))
                            (activity as? MainActivity)?.restart()
                        }
                    }
                }
            }
        }

        val notLoadingFlow = uiState.map { it.loadState }
            .map { it.refresh !is LoadState.Loading }
        val hasErrorFlow = uiState.map { it.exception != null }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                notLoadingFlow,
                hasErrorFlow,
                Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorText
                    if (e != null) {
                        if (BuildConfig.DEBUG) {
                            Timber.e(e)
                        }
                        if (uiErr != null) {
                            context?.showToast(uiErr.asString(requireContext()))
                        }
                        uiAction(SubscriptionSuccessUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                if (loadState.refresh is LoadState.Loading) {
                    nextButton.setSpinning()
                } else {
                    nextButton.cancelSpinning()
                    if (loadState.refresh is LoadState.Error) {
                        nextButton.shakeNow()
                    }
                }
            }
        }

        /*nextButton.setOnClickListener {
            viewModel.generateAvatarRequest(planId)
        }*/

        val delta = System.currentTimeMillis() - pagePresentedAt
        viewLifecycleOwner.lifecycleScope.launch {
            delay((UI_PRESENTATION_TIME - delta).coerceAtLeast(0))
            nextButton.cancelSpinning()
            (activity as? MainActivity)?.restart()
        }
        nextButton.setSpinning()
    }

    companion object {
        private const val UI_PRESENTATION_TIME = 5000L
    }

}