package com.aiavatar.app.feature.onboard.presentation.login

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.Constant
import com.aiavatar.app.MainActivity
import com.aiavatar.app.R
import com.aiavatar.app.commons.presentation.dialog.SimpleDialog
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.commons.util.InvalidOtpException
import com.aiavatar.app.commons.util.ResolvableException
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.databinding.FragmentLoginBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.hideKeyboard
import com.aiavatar.app.showSoftInputMode
import com.aiavatar.app.showToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
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
import kotlin.math.sign


@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient
    private val googleSignInResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }

    private var from: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        from = arguments?.getString(Constant.EXTRA_FROM)
        Timber.d("From: $from")
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
            uiState = viewModel.uiState, uiAction = viewModel.accept, uiEvent = viewModel.uiEvent
        )
        handleBackPressed()
        checkLastSignedInAccount()
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
                        when (from) {
                            "avatar_result" -> {
                                findNavController().apply {
                                    val args = bundleOf(
                                        Constant.EXTRA_FROM to "login"
                                    )
                                    val navOpts = NavOptions.Builder()
                                        .setEnterAnim(R.anim.fade_scale_in)
                                        .setExitAnim(R.anim.fade_scale_out)
                                        .setPopUpTo(R.id.login_fragment, inclusive = true, saveState = false)
                                        .build()
                                    navigate(R.id.subscription_plans, args, navOpts)
                                }
                            }
                            "create_masterpiece" -> {
                                findNavController().apply {
                                    val args = bundleOf(
                                        Constant.EXTRA_FROM to "login"
                                    )
                                    val navOpts = NavOptions.Builder()
                                        .setEnterAnim(R.anim.fade_scale_in)
                                        .setExitAnim(R.anim.fade_scale_out)
                                        .setPopUpTo(R.id.landingPage, inclusive = true, saveState = false)
                                        .build()
                                    navigate(R.id.upload_step_1, args, navOpts)
                                }
                            }
                            else -> {
                                gotoHome()
                            }
                        }
                    }
                }
            }
        }

        val notLoadingFlow = uiState.map { it.loadState }.distinctUntilChanged { old, new ->
                old.refresh == new.refresh && old.action == new.action
            }.map { it.action !is LoadState.Loading && it.refresh !is LoadState.Loading }


        val hasErrorFlow = uiState.map { it.exception != null }.distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                notLoadingFlow, hasErrorFlow, Boolean::and
            ).collectLatest { hasError ->
                if (hasError) {
                    val (e, uiErr) = uiState.value.exception to uiState.value.uiErrorMessage
                    if (e != null) {
                        if (uiErr != null) {
                            context?.showToast(uiErr.asString(requireContext()))
                        }
                        when (e) {
                            is ResolvableException -> {
                                if (uiState.value.loginSequence == LoginSequence.OTP_SENT) {
                                    edOtp.shakeNow()
                                } else {
                                    edEmail.shakeNow()
                                }
                                HapticUtil.createError(requireContext())
                            }

                            else -> {
                                when (e.cause) {
                                    is InvalidOtpException -> {
                                        edOtp.shakeNow()
                                        HapticUtil.createError(requireContext())
                                    }
                                }
                            }
                        }
                        uiAction(LoginUiAction.ErrorShown(e))
                    }
                }
            }
        }

        val loadStateFlow = uiState.map { it.loadState }.distinctUntilChangedBy { it.action }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                if (loadState.action is LoadState.Loading) {
                    nextButton.setSpinning()
                } else {
                    nextButton.cancelSpinning()
                }
            }
        }

        val loginSequenceFlow = uiState.map { it.loginSequence }.distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            loginSequenceFlow.collectLatest { loginSequence ->
                when (loginSequence) {
                    LoginSequence.TYPING_EMAIL -> {
                        edOtp.isVisible = false
                        edEmail.isEnabled = true
                        textOr.isVisible = true
                        socialLoginContainer.isVisible = true
                        hideBackButton()
                    }

                    LoginSequence.OTP_SENT -> {
                        edOtp.isVisible = true
                        edEmail.isEnabled = false
                        textOr.isVisible = false
                        socialLoginContainer.isVisible = false

                        /*edOtp.requestFocus()
                        edOtp.requestFocusFromTouch()
                        edOtp.showSoftInputMode()*/
                        showBackButton()
                    }

                    LoginSequence.OTP_VERIFIED -> { /* Noop */
                    }
                }
            }
        }

        bindInput(
            uiState, uiAction
        )

        bindClick(
            uiState = uiState, uiAction = uiAction
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

        edEmail.addTextChangedListener(afterTextChanged = { updateTypedEmailValue(uiAction) })

        edOtp.addTextChangedListener(afterTextChanged = { updateTypedOtpValue(uiAction) })
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
        onTyped: (LoginUiAction.TypingOtp) -> Unit,
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

        cardBackButton.setOnClickListener {
            Timber.d("Back pressed!")
            viewModel.handleBackPressed()
        }

        edOtp.setOnClickListener {
            edOtp.requestFocus()
            edOtp.requestFocusFromTouch()
            edOtp.showSoftInputMode()
        }

        btnSignInGoogle.setOnClickListener {
            googleSignIn()
        }
    }

    private fun FragmentLoginBinding.showBackButton() {
        val rotationAnim = RotateAnimation(
            180f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            interpolator = DecelerateInterpolator()
            duration = 200L
            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    cardBackButton.isVisible = true
                }

                override fun onAnimationEnd(animation: Animation?) {
                    cardBackButton.animation = null
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // Noop
                }
            })
        }
        val fadeInAnim = AlphaAnimation(0F, 1F).apply {
            duration = 200L
            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    cardBackButton.alpha = 0F
                }

                override fun onAnimationEnd(animation: Animation?) {
                    cardBackButton.alpha = 1.0F
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // Noop
                }
            })
        }
        val animSet = AnimationSet(true)
        animSet.addAnimation(rotationAnim)
        animSet.addAnimation(fadeInAnim)
        cardBackButton.startAnimation(animSet)
    }

    private fun FragmentLoginBinding.hideBackButton() {
        val rotationAnim = RotateAnimation(
            0F, -90F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            interpolator = DecelerateInterpolator()
            duration = 200L
            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Noop
                }

                override fun onAnimationEnd(animation: Animation?) {
                    cardBackButton.isVisible = false
                    cardBackButton.animation = null
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // Noop
                }
            })
        }
        val fadeOutAnim = AlphaAnimation(1F, 0F).apply {
            duration = 200L
            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Noop
                }

                override fun onAnimationEnd(animation: Animation?) {
                    cardBackButton.alpha = 1.0F
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // Noop
                }
            })
        }
        val animSet = AnimationSet(true)
        animSet.addAnimation(rotationAnim)
        animSet.addAnimation(fadeOutAnim)
        cardBackButton.startAnimation(animSet)
    }

    private fun handleBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!viewModel.handleBackPressed()) {
                        if (!findNavController().navigateUp()) {
                            activity?.finish()
                        }
                    }
                }
            })
    }

    private fun checkLastSignedInAccount() {
        // TODO: Check last singed in account and confirm user
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            SimpleDialog(
                context = requireContext(),
                titleText = "Google Sign In",
                message = "You have already signed in with your Google account in this app. Continue as ${account.email}?",
                positiveButtonText = "Sign In",
                positiveButtonAction = {
                    updateUI(account)
                },
                negativeButtonText = "Cancel",
                negativeButtonAction = {
                    googleSignInClient.signOut()
                },
                cancellable = false,
                showCancelButton = false
            ).show()
        }
    }

    private fun googleSignIn() {
        val signInIntent: Intent = googleSignInClient.signInIntent
        googleSignInResultLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            android.util.Log.w(TAG, "signInResult:failed code=" + e.getStatusCode())
            updateUI(null)
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        Timber.d("Sign in: Google id = ${account?.id} email = ${account?.email}")
        if (account != null) {
            if (account.id != null && account.email != null) {
                viewModel.socialLogin("google", account.id!!, email = account.email!!,
                    photoUrl = account.photoUrl?.toString())
            } else {
                context?.showToast("Cannot sign in with Google right now. Try other methods.")
                googleSignInClient.signOut()
            }
        }
    }

    private fun animateAndEnd(view: View) {
        (view as ImageView).setImageDrawable(null)
        view.animate().scaleX(50f).scaleY(50f).setDuration(500)
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
            }).start()
    }

    private fun gotoHome() {
        activity?.apply {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            intent.putExtra("restart_hint", "from_login")
            startActivity(intent)
        }
    }

    companion object {
        private val TAG = LoginFragment::class.java.simpleName
        const val RC_SIGN_IN = 100
    }

}