package com.pepulai.app.feature.onboard.presentation.login

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pepulai.app.MainActivity
import com.pepulai.app.R
import com.pepulai.app.commons.util.AnimationUtil.shakeNow
import com.pepulai.app.commons.util.HapticUtil
import com.pepulai.app.commons.util.ResolvableException
import com.pepulai.app.commons.util.cancelSpinning
import com.pepulai.app.commons.util.setSpinning
import com.pepulai.app.databinding.FragmentLoginBinding
import com.pepulai.app.hideKeyboard
import com.pepulai.app.showToast
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.log

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentLoginBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
        handleBackPressed()
    }

    private fun FragmentLoginBinding.bindState(
        uiState: StateFlow<LoginState>,
        uiAction: (LoginUiAction) -> Unit,
        uiEvent: SharedFlow<LoginUiEvent>,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is LoginUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }

                    is LoginUiEvent.NextScreen -> {
                        // animateAndEnd(nextButton)
                        gotoHome()
                    }
                }
            }
        }

        val notLoadingFlow = uiState.map { it.loadState }
            .distinctUntilChanged { old, new ->
                old.refresh == new.refresh && old.action == new.action
            }
            .map { it.action !is LoadState.Loading && it.refresh !is LoadState.Loading }


        val hasErrorFlow = uiState.map { it.exception != null }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                notLoadingFlow,
                hasErrorFlow,
                Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorMessage
                    if (e != null) {
                        if (uiErr != null) {
                            context?.showToast(uiErr.asString(requireContext()))
                        }
                        when (e) {
                            is ResolvableException -> {
                                edEmail.shakeNow()
                                HapticUtil.createError(requireContext())
                            }
                        }
                        uiAction(LoginUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChangedBy { it.action }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                if (loadState.action is LoadState.Loading) {
                    nextButton.setSpinning()
                } else {
                    nextButton.cancelSpinning()
                }
            }
        }

        val loginSequenceFlow = uiState.map { it.loginSequence }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loginSequenceFlow.collectLatest { loginSequence ->
                when (loginSequence) {
                    LoginSequence.TYPING_EMAIL -> {
                        edOtp.isVisible = false
                        edEmail.isEnabled = true
                    }
                    LoginSequence.OTP_SENT -> {
                        edOtp.isVisible = true
                        edEmail.isEnabled = false
                    }
                    LoginSequence.OTP_VERIFIED -> { /* Noop */ }
                }
            }
        }

        bindInput(
            uiState,
            uiAction
        )

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )
    }

    private fun FragmentLoginBinding.bindInput(
        uiState: StateFlow<LoginState>,
        uiAction: (LoginUiAction) -> Unit,
    ) {
        edEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updateTypedEmailValue(uiAction)
                uiAction(LoginUiAction.NextClick)
                edEmail.hideKeyboard()
                true
            } else {
                false
            }
        }

        edEmail.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateTypedEmailValue(uiAction)
                uiAction(LoginUiAction.NextClick)
                edEmail.hideKeyboard()
                true
            } else {
                false
            }
        }

        edEmail.addTextChangedListener(
            afterTextChanged = { updateTypedEmailValue(uiAction) }
        )

        edOtp.addTextChangedListener(
            afterTextChanged = { updateTypedOtpValue(uiAction)  }
        )
    }

    private fun FragmentLoginBinding.updateTypedEmailValue(
        onTyped: (LoginUiAction.TypingUsername) -> Unit,
    ) {
        edEmail.text.toString().trim().let {
            if (it.isNotEmpty()) {
                onTyped(LoginUiAction.TypingUsername(typed = it))
            }
        }
    }

    private fun FragmentLoginBinding.updateTypedOtpValue(
        onTyped: (LoginUiAction.TypingOtp) -> Unit
    ) {
        edOtp.text.toString().trim().let {
            if (it.isNotEmpty()) {
                onTyped(LoginUiAction.TypingOtp(typed = it))
            }
        }
    }

    private fun FragmentLoginBinding.bindClick(
        uiState: StateFlow<LoginState>,
        uiAction: (LoginUiAction) -> Unit,
    ) {
        nextButton.setOnClickListener {
            uiAction(LoginUiAction.NextClick)
        }
    }

    private fun handleBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    activity?.finish()
                }
            })
    }

    private fun animateAndEnd(view: View) {
        (view as ImageView).setImageDrawable(null)
        view.animate()
            .scaleX(50f)
            .scaleY(50f)
            .setDuration(500)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    // Noop
                }

                override fun onAnimationEnd(p0: Animator) {
                    gotoHome()
                }

                override fun onAnimationCancel(p0: Animator) {
                    // Noop
                }

                override fun onAnimationRepeat(p0: Animator) {
                    // Noop
                }
            })
            .start()
    }

    private fun gotoHome() {
        activity?.apply {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            intent.putExtra("restart_hint", "from_login")
            startActivity(intent)
        }
    }

}