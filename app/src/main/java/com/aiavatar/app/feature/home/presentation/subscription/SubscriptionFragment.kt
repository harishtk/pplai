package com.aiavatar.app.feature.home.presentation.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.*
import com.aiavatar.app.commons.util.ResolvableException
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.commons.util.shakeNow
import com.aiavatar.app.databinding.FragmentSubscriptionBinding
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.presentation.util.SubscriptionPlanAdapter
import com.aiavatar.app.feature.onboard.presentation.login.LoginFragment
import com.aiavatar.app.viewmodels.UserViewModel
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SubscriptionFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels()
    private val viewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            getString(Constant.ARG_MODEL_ID, null)?.let { modelId ->
                viewModel.setModelId(modelId)
            } ?: error("No model id available")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_subscription, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSubscriptionBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        setupObservers()
    }

    private fun FragmentSubscriptionBinding.bindState(
        uiState: StateFlow<SubscriptionState>,
        uiAction: (SubscriptionUiAction) -> Unit,
        uiEvent: SharedFlow<SubscriptionUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            uiEvent.collectLatest { event ->
                when (event) {
                    is SubscriptionUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                    is SubscriptionUiEvent.PurchaseComplete -> {
                        findNavController().apply {
                            val args = bundleOf(
                                Constant.EXTRA_FROM to "login",
                                Constant.ARG_PLAN_ID to event.planId,
                                Constant.ARG_STATUS_ID to event.statusId
                            )
                            val navOpts = defaultNavOptsBuilder()
                                .setPopUpTo(R.id.subscription_plans, inclusive = true, saveState = false)
                                .build()
                            navigate(R.id.subscriptionSuccess, args, navOpts)
                        }
                    }
                }
            }
        }

        val notLoadingFlow = uiState.map { it.loadState }
            .map { it.refresh !is LoadState.Loading }
        val hasErrorsFlow = uiState.map { it.exception != null }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                notLoadingFlow,
                hasErrorsFlow,
                Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorText
                    if (e != null) {
                        if (uiErr != null) {
                            context?.showToast(uiErr.asString(requireContext()))
                        }
                        when (e) {
                            is ResolvableException -> {
                                btnNext.shakeNow()
                            }
                        }
                        uiAction(SubscriptionUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChangedBy { it.refresh }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                progressBar.isVisible = loadState.refresh is LoadState.Loading
                if (loadState.action is LoadState.Loading) {
                    btnNext.setSpinning()
                } else {
                    btnNext.cancelSpinning()
                }
            }
        }

        val subscriptionAdapterCallback = object : SubscriptionPlanAdapter.Callback {
            override fun onItemClick(position: Int) {
                // Noop
            }

            override fun onSelectPlan(position: Int, plan: SubscriptionPlan) {
                Timber.d("onSelectPlan: $position ${plan.price}")
                uiAction(SubscriptionUiAction.ToggleSelectedPlan(planId = plan.id))
            }
        }
        val subscriptionPlanAdapter = SubscriptionPlanAdapter(subscriptionAdapterCallback)

        bindList(
            adapter = subscriptionPlanAdapter,
            uiState = uiState
        )

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )
    }

    private fun FragmentSubscriptionBinding.bindList(
        adapter: SubscriptionPlanAdapter,
        uiState: StateFlow<SubscriptionState>
    ) {
        packageList.adapter = adapter

        val subscriptionPlansListFlow = uiState.map { it.subscriptionPlans }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            subscriptionPlansListFlow.collectLatest { subscriptionPlansList ->
                adapter.submitList(subscriptionPlansList)
            }
        }
    }

    private fun FragmentSubscriptionBinding.bindClick(
        uiState: StateFlow<SubscriptionState>,
        uiAction: (SubscriptionUiAction) -> Unit
    ) {
        btnNext.setOnClickListener {
            uiAction(SubscriptionUiAction.NextClick)
        }

        btnClose.setOnClickListener {
            try { findNavController().navigateUp() }
            catch (ignore: Exception) {}
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.loginUser.collectLatest { loginUser ->
                if (loginUser == null) {
                    gotoLogin()
                } else {
                    if (loginUser.userId != null) {
                        viewModel.refresh()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                getNavigationResultFlow<Boolean>(LoginFragment.LOGIN_RESULT)?.collectLatest { isLoggedIn ->
                    if (isLoggedIn != null) {
                        Timber.d("Login result: $isLoggedIn")
                        /*if (isLoggedIn != true) {
                            safeCall { findNavController().navigateUp() }
                        }*/
                        clearNavigationResult<Boolean>(LoginFragment.LOGIN_RESULT)
                    }
                }
            }
        }
    }

    private fun gotoLogin() {
        Timber.d("User login: opening login..")
        findNavController().apply {
            val navOpts = defaultNavOptsBuilder().build()
            val args = Bundle().apply {
                /* 'popup' means this page, the one who fired it expects the result */
                putString(Constant.EXTRA_FROM, "popup")
                putInt(Constant.EXTRA_POP_ID, R.id.subscription_plans)
            }
            navigate(R.id.login_fragment, args, navOpts)
        }
    }
}