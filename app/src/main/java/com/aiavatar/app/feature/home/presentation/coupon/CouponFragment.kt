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
import com.aiavatar.app.databinding.FragmentCouponBinding
import com.aiavatar.app.feature.onboard.presentation.login.LoginUiAction
import com.aiavatar.app.hideKeyboard
import com.aiavatar.app.showToast
import com.mukesh.mukeshotpview.completeListener.MukeshOtpCompleteListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.TimeoutCancellationException
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
                    couponInputLayout.isErrorEnabled = true
                    couponInputLayout.error = couponValidationResult.errorMessage?.asString(requireContext())
                } else {
                    couponInputLayout.isErrorEnabled = false
                    couponInputLayout.error = null
                }
            }
        }

        bindInput(
            uiState = uiState,
            uiAction = uiAction
        )

        bindToolbar()
    }

    private fun FragmentCouponBinding.bindInput(
        uiState: StateFlow<CouponState>,
        uiAction: (CouponUiAction) -> Unit,
    ) {
        edCoupon.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updateTypedCouponValue(uiAction)
                uiAction(CouponUiAction.NextClick)
                edCoupon.hideKeyboard()
                true
            } else {
                false
            }
        }

        edCoupon.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateTypedCouponValue(uiAction)
                uiAction(CouponUiAction.NextClick)
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

    private fun FragmentCouponBinding.bindToolbar() {
        toolbarIncluded.toolbarTitle.setText("Claim Coupon")
        toolbarIncluded.toolbarNavigationIcon.apply {
            isVisible = true
            setOnClickListener { findNavController().navigateUp() }
        }
    }

}