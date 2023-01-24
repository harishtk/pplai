package com.aiavatar.app.feature.home.presentation.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.*
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.setSpinning
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
        handleBackPressed()
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
                            delay(UI_PRESENTATION_TIME)
                            gotoAvatarStatus()
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

        /*val loadStateFlow = uiState.map { it.loadState }
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
        }*/

        /*nextButton.setOnClickListener {
            viewModel.generateAvatarRequest(planId)
        }*/

        nextButton.postDelayed({
            nextButton.isVisible = true
            nextButton.setSpinning()
        }, UI_RENDER_WAIT_TIME)

        viewLifecycleOwner.lifecycleScope.launch {
            delay(UI_PRESENTATION_TIME)
            nextButton.cancelSpinning()
            gotoAvatarStatus()
        }
    }

    private fun gotoAvatarStatus() = safeCall {
        findNavController().apply {
            val navOpts = NavOptions.Builder()
                .setLaunchSingleTop(true)
                /*.setEnterAnim(R.anim.slide_from_top)
                .setExitAnim(R.anim.slide_to_top)*/
                .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                .build()
            navigate(R.id.avatar_status, null, navOpts)
        }
    }

    private fun handleBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing. The page automatically closes
                }
            })
    }

    companion object {
        private const val UI_PRESENTATION_TIME = 5000L
        private const val UI_RENDER_WAIT_TIME = 100L
    }

}