package com.aiavatar.app.feature.home.presentation.coupon

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.databinding.FragmentCouponBinding
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.presentation.util.SubscriptionPlanAdapter
import com.aiavatar.app.hideKeyboard
import com.aiavatar.app.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 * @author Hariskumar Kubendran
 * @date 27/02/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
@AndroidEntryPoint
class CouponFragment : Fragment() {

    private val viewModel: CouponViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_coupon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCouponBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiEvent = viewModel.uiEvent,
            uiAction = viewModel.accept
        )
    }

    private fun FragmentCouponBinding.bindState(
        uiState: StateFlow<CouponState>,
        uiEvent: SharedFlow<CouponUiEvent>,
        uiAction: (CouponUiAction) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                uiEvent.collectLatest { event ->
                    when (event) {
                        is CouponUiEvent.ShowToast -> {
                            context?.showToast(event.message.asString(requireContext()))
                        }
                    }
                }
            }
        }

        val couponValidationResultFlow = uiState.map { it.couponValidationResult }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            couponValidationResultFlow.collectLatest { couponValidationResult ->
                Timber.d("Validation Result: (coupon) = $couponValidationResult")
                if (couponValidationResult != null) {
                    if (couponValidationResult.successful) {
                        couponInputLayout.isEndIconVisible = true
                        btnVerify.isVisible = false
                        edCoupon.clearFocus()
                        edCoupon.hideKeyboard()
                    } else {
                        btnVerify.isVisible = true
                        couponInputLayout.isEndIconVisible = false
                        couponInputLayout.isErrorEnabled = true
                        couponInputLayout.error =
                            couponValidationResult.errorMessage?.asString(requireContext())
                    }
                } else {
                    btnVerify.isVisible = true
                    couponInputLayout.isEndIconVisible = false
                    couponInputLayout.isErrorEnabled = false
                    couponInputLayout.error = null
                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChangedBy { it.refresh }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                Timber.d("Load state: $loadState")
                smallProgressBar.isVisible = loadState.refresh is LoadState.Loading
            }
        }

        val cb = object : SubscriptionPlanAdapter.Callback {
            override fun onItemClick(position: Int) {
                // Noop.
            }

            override fun onSelectPlan(position: Int, plan: SubscriptionPlan) {
                viewModel.toggleSelection(plan.id)
            }

            override fun onFooterClick(position: Int) {
                /* Noop */
            }
        }
        val adapter = SubscriptionPlanAdapter(cb)

        bindList(
            adapter = adapter,
            uiState = uiState,
            uiAction = uiAction
        )

        bindInput(
            uiState = uiState,
            uiAction = uiAction
        )

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )

        bindToolbar()
    }

    private fun FragmentCouponBinding.bindClick(
        uiState: StateFlow<CouponState>,
        uiAction: (CouponUiAction) -> Unit
    ) {
        btnVerify.setOnClickListener {
            uiAction(CouponUiAction.Validate)
        }

        btnNext.setOnClickListener {
            uiAction(CouponUiAction.NextClick)
        }

        val selectedPlanIdFlow = uiState.map { it.selectedPlanId }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            selectedPlanIdFlow.collectLatest { selectedPlanId ->
                // TODO: show continue button
                Timber.d("selectedPlanId: $selectedPlanId")
                btnNext.isVisible = selectedPlanId != -1
            }
        }
    }

    private fun FragmentCouponBinding.bindInput(
        uiState: StateFlow<CouponState>,
        uiAction: (CouponUiAction) -> Unit,
    ) {
        edCoupon.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                updateTypedCouponValue(uiAction)
                uiAction(CouponUiAction.Validate)
                edCoupon.hideKeyboard()
                true
            } else {
                false
            }
        }

        edCoupon.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateTypedCouponValue(uiAction)
                uiAction(CouponUiAction.Validate)
                edCoupon.hideKeyboard()
                true
            } else {
                false
            }
        }

        edCoupon.addTextChangedListener(afterTextChanged = { updateTypedCouponValue(uiAction) })
    }

    private fun FragmentCouponBinding.updateTypedCouponValue(
        onTyped: (CouponUiAction.TypingCoupon) -> Unit,
    ) {
        edCoupon.text.toString().trim().let {
            if (it.isNotBlank()) {
                onTyped(CouponUiAction.TypingCoupon(typed = it))
            }
        }
    }

    private fun FragmentCouponBinding.bindList(
        adapter: SubscriptionPlanAdapter,
        uiState: StateFlow<CouponState>,
        uiAction: (CouponUiAction) -> Unit
    ) {
        plansListView.adapter = adapter

        val uiModelListFlow = uiState.map { it.uiModelList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            uiModelListFlow.collectLatest { uiModelList ->
                Timber.d("Plans: size = ${uiModelList.size} $uiModelList")
                adapter.submitList(uiModelList)
            }
        }
    }

    private fun FragmentCouponBinding.bindToolbar() {
        toolbarIncluded.toolbarTitle.setText("Claim Coupon")
        toolbarIncluded.toolbarNavigationIcon.apply {
            isVisible = true
            setOnClickListener { findNavController().navigateUp() }
        }
    }

}