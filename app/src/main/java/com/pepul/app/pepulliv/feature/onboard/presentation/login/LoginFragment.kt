package com.pepul.app.pepulliv.feature.onboard.presentation.login

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pepul.app.pepulliv.MainActivity
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.commons.util.AnimationUtil.shakeNow
import com.pepul.app.pepulliv.commons.util.HapticUtil
import com.pepul.app.pepulliv.commons.util.ResolvableException
import com.pepul.app.pepulliv.databinding.FragmentLoginBinding
import com.pepul.app.pepulliv.di.ApplicationDependencies
import com.pepul.app.pepulliv.hideKeyboard
import com.pepul.app.pepulliv.showToast
import com.pepulnow.app.data.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
    }

    private fun FragmentLoginBinding.bindState(
        uiState: StateFlow<LoginState>,
        uiAction: (LoginUiAction) -> Unit,
        uiEvent: SharedFlow<LoginUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is LoginUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                    is LoginUiEvent.NextScreen -> {
                        animateAndEnd(nextButton)
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
                                edUsername.shakeNow()
                                HapticUtil.createError(requireContext())
                            }
                        }
                        uiAction(LoginUiAction.ErrorShown(e))
                    }
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
        uiAction: (LoginUiAction) -> Unit
    ) {
        edUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updateTypedValue(uiAction)
                uiAction(LoginUiAction.NextClick)
                edUsername.hideKeyboard()
                true
            } else {
                false
            }
        }

        edUsername.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateTypedValue(uiAction)
                uiAction(LoginUiAction.NextClick)
                edUsername.hideKeyboard()
                true
            } else {
                false
            }
        }

        edUsername.addTextChangedListener(
            afterTextChanged = { updateTypedValue(uiAction) }
        )
    }

    private fun FragmentLoginBinding.updateTypedValue(
        onTyped: (LoginUiAction.TypingUsername) -> Unit
    ) {
        edUsername.text.toString().trim().let {
            if (it.isNotEmpty()) {
                onTyped(LoginUiAction.TypingUsername(typed = it))
            }
        }
    }

    private fun FragmentLoginBinding.bindClick(
        uiState: StateFlow<LoginState>,
        uiAction: (LoginUiAction) -> Unit
    ) {
        nextButton.setOnClickListener {
            uiAction(LoginUiAction.NextClick)
        }
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
            startActivity(intent)
        }
    }

}