package com.aiavatar.app.feature.home.presentation.subscription

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.*
import com.aiavatar.app.commons.presentation.dialog.SimpleDialog
import com.aiavatar.app.commons.util.*
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.AnimationUtil.touchInteractFeedback
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.core.Env
import com.aiavatar.app.core.data.source.local.entity.*
import com.aiavatar.app.core.envForConfig
import com.aiavatar.app.databinding.FragmentSubscriptionBinding
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.presentation.payments.PaymentMethod
import com.aiavatar.app.feature.home.presentation.payments.PaymentMethodData
import com.aiavatar.app.feature.home.presentation.payments.PaymentMethodSheet
import com.aiavatar.app.feature.home.presentation.util.EmptyInAppProductsException
import com.aiavatar.app.feature.home.presentation.util.SubscriptionPlanAdapter
import com.aiavatar.app.feature.onboard.presentation.login.LoginFragment
import com.aiavatar.app.pay.billing.InAppPurchaseActivity
import com.aiavatar.app.pay.billing.InAppUtil
import com.aiavatar.app.pay.billing.ResultCode
import com.aiavatar.app.pay.billing.queryPurchases
import com.aiavatar.app.viewmodels.UserViewModel
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.Purchase.PurchaseState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.immutableListOf
import timber.log.Timber
import java.util.concurrent.CancellationException

@AndroidEntryPoint
class SubscriptionFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val viewModel: SubscriptionViewModel by viewModels()

    private val inAppPurchaseResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // TODO: Handle in-app purchase results
        Timber.d("Payment: resultCode = ${result.resultCode} extras = ${result.data?.extras?.toString()}")
        Timber.d("Payment: debug message = ${result.data?.getStringExtra(InAppPurchaseActivity.EXTRA_DEBUG_MESSAGE)}")
        when (result.resultCode) {
            ResultCode.SUCCESS -> {
                transactionId?.let { txnId ->
                    viewModel.updatePaymentLog(
                        transactionId = txnId,
                        paymentStatus = PAYMENT_STATUS_COMPLETE,
                        purchaseToken = result.data?.getStringExtra(InAppPurchaseActivity.EXTRA_PURCHASE_TOKEN).nullAsEmpty()
                    )
                }
                result.data?.getStringExtra(InAppPurchaseActivity.EXTRA_MESSAGE)?.let { message ->
                    context?.showToast(message)
                }

                val purchaseToken = result.data?.getStringExtra(InAppPurchaseActivity.EXTRA_PURCHASE_TOKEN)
                    .nullAsEmpty()
                viewModel.sendPurchaseDetailsToServer(purchaseToken)
            }
            ResultCode.USER_CANCELED -> {
                transactionId?.let { txnId ->
                    viewModel.updatePaymentLog(
                        transactionId = txnId,
                        paymentStatus = PAYMENT_STATUS_CANCELED,
                        purchaseToken = result.data?.getStringExtra(InAppPurchaseActivity.EXTRA_PURCHASE_TOKEN).nullAsEmpty()
                    )
                }
            }
            ResultCode.ERROR, ResultCode.FAILED -> {
                transactionId?.let { txnId ->
                    viewModel.updatePaymentLog(
                        transactionId = txnId,
                        paymentStatus = PAYMENT_STATUS_FAILED,
                        purchaseToken = result.data?.getStringExtra(InAppPurchaseActivity.EXTRA_PURCHASE_TOKEN).nullAsEmpty()
                    )
                }
            }
            ResultCode.PENDING -> {
                transactionId?.let { txnId ->
                    viewModel.updatePaymentLog(
                        transactionId = txnId,
                        paymentStatus = PAYMENT_STATUS_FAILED,
                        purchaseToken = result.data?.getStringExtra(InAppPurchaseActivity.EXTRA_PURCHASE_TOKEN).nullAsEmpty()
                    )
                }
            }
            else -> {
                when (result.resultCode) {
                    ResultCode.PENDING -> {
                        // checkPendingPurchases()

                    }
                }
                result.data?.getStringExtra(InAppPurchaseActivity.EXTRA_ERROR_MESSAGE)?.let { message ->
                    context?.showToast(message)
                }
            }
        }
    }

    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        // TODO: partially implemented
        Timber.d("purchaseUpdatedListener: ${billingResult.responseCode} msg = ${billingResult.debugMessage}")
    }

    private val billingClient by lazy {
        BillingClient.newBuilder(requireContext())
            .enablePendingPurchases()
            .setListener(purchaseUpdatedListener)
            .build()
    }

    private var pendingDialogWeakRef: Dialog? = null

    // Helper flags
    private var isBillingClientConnected: Boolean = false
    private var shouldRetryPendingPurchases: Boolean = false
    private var paymentProcessing = false
    private var transactionId: String? = null

    private var pendingPurchaseFetchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            getString(Constant.ARG_MODEL_ID, null)?.let { modelId ->
                viewModel.setModelId(modelId)
            } ?: error("No model id available")
        }

        initBillingClientIfRequired()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
        uiEvent: SharedFlow<SubscriptionUiEvent>,
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
                                .setPopUpTo(
                                    R.id.subscription_plans,
                                    inclusive = true,
                                    saveState = false
                                )
                                .build()
                            navigate(R.id.subscriptionSuccess, args, navOpts)
                        }
                    }
                }
            }
        }

        val text = "I have a coupon code"

        btnIHaveCouponCode.makeLinks(listOf(
            text to {
                context?.showToast("Coupon code")
            }
        ))

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
                                // HapticUtil.createError(requireContext())
                            }
                            is EmptyInAppProductsException -> {
                                tvEmptyList.text = uiErr?.asString(requireContext())
                            }
                            is ApiException -> {
                                tvEmptyList.text = "Oops! There was a problem fetching the plans."
                            }
                            is NoInternetException -> {
                                tvEmptyList.text = "Please connect to the internet and try again."
                            }
                        }
                        uiAction(SubscriptionUiAction.ErrorShown(e))
                    }
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

            override fun onFooterClick(position: Int) {

            }
        }
        val subscriptionPlanAdapter = SubscriptionPlanAdapter(subscriptionAdapterCallback)

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChangedBy { it.refresh }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                val emptyList = subscriptionPlanAdapter.itemCount <= 0
                progressBar.isVisible = loadState.refresh is LoadState.Loading
                retryButton.isVisible = loadState.refresh is LoadState.Error &&
                        emptyList
                emptyListContainer.isVisible = loadState.refresh is LoadState.Error &&
                        emptyList

                if (loadState.action is LoadState.Loading) {
                    btnNext.setSpinning()
                } else {
                    if (loadState.refresh is LoadState.Error) {
                        if (retryButton.isVisible) {
                            if (loadState.refresh.error !is NoInternetException) {
                                HapticUtil.createError(requireContext())
                            }
                            retryButton.shakeNow()
                        }
                    }
                    btnNext.cancelSpinning()
                }
            }
        }

        bindProductQuery(
            uiState = uiState,
            uiAction = uiAction
        )

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
        uiState: StateFlow<SubscriptionState>,
    ) {
        packageList.adapter = adapter

        val subscriptionPlansListFlow = uiState.map { it.subscriptionPlansUiModels }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            subscriptionPlansListFlow.collectLatest { subscriptionPlansList ->
                adapter.submitList(subscriptionPlansList)

                btnNext.isVisible = subscriptionPlansList.isNotEmpty()
                tvIHaveCouponCode.isVisible = subscriptionPlansList.isNotEmpty()
            }
        }
    }

    private fun FragmentSubscriptionBinding.bindClick(
        uiState: StateFlow<SubscriptionState>,
        uiAction: (SubscriptionUiAction) -> Unit,
    ) {
        btnNext.setOnClickListener {
            if (!paymentProcessing) {
                if (viewModel.validate()) {
                    confirmPaymentMethod(uiAction)
                }
            }
            // uiAction(SubscriptionUiAction.NextClick)
        }

        btnClose.setOnClickListener {
            try {
                findNavController().navigateUp()
            } catch (ignore: Exception) {
            }
        }

        val couponCodeString = getString(R.string.i_have_coupon_code)
        val sb = SpannableString(couponCodeString).also {
            it.setSpan(UnderlineSpan(), 0, it.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        tvIHaveCouponCode.setText(sb, TextView.BufferType.SPANNABLE)
        tvIHaveCouponCode.setOnClickListener {
            it.touchInteractFeedback(scaleMultiplier = 1.1F)
            gotoClaimCoupon()
        }

        retryButton.setOnClickListener { viewModel.refresh() }
    }

    private fun FragmentSubscriptionBinding.bindProductQuery(
        uiState: StateFlow<SubscriptionState>,
        uiAction: (SubscriptionUiAction) -> Unit
    ) {
        val billingConnectionStateFlow = uiState.map { it.billingConnectionState }
            .distinctUntilChanged()
        val internalSubscriptionPlansFlow = uiState.map { it.subscriptionPlansCache }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                billingConnectionStateFlow,
                internalSubscriptionPlansFlow,
                ::Pair
            ).collectLatest { (connected, internalPlans) ->
                Timber.d("Plans: connected = $connected internalPlans = $internalPlans")
                if (connected) {
                    if (internalPlans != null) {
                        viewModel.setLoading(LoadType.REFRESH, LoadState.Loading())

                        val productList = ArrayList<QueryProductDetailsParams.Product>()
                        internalPlans.forEach { plan ->
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(plan.productId)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build().also {
                                    productList.add(it)
                                }
                        }

                        val params = QueryProductDetailsParams.newBuilder()
                            .setProductList(productList)
                            .build()

                        val productDetailsResult = withContext(Dispatchers.IO) {
                            Timber.d("Environment: ${envForConfig(BuildConfig.ENV)} config = ${BuildConfig.ENV}")
                            if (envForConfig(BuildConfig.ENV) == Env.DEV) {
                                ProductDetailsResult(
                                    billingResult = BillingResult.newBuilder()
                                        .setResponseCode(BillingResponseCode.ERROR)
                                        .build(),
                                    productDetailsList = null
                                )
                            } else {
                                billingClient.queryProductDetails(params)
                            }
                        }

                        val productDetailsList = productDetailsResult.productDetailsList

                        if (productDetailsResult.billingResult.responseCode == BillingResponseCode.OK
                            && productDetailsList != null && productDetailsList.isNotEmpty()) {

                            val uiModelList = internalPlans.intersect(productDetailsList) { l, r ->
                                l.productId == r.productId
                            }.map { plan ->
                                if (plan.bestSeller) {
                                    uiAction(SubscriptionUiAction.ToggleSelectedPlan(plan.id))
                                }
                                SubscriptionUiModel.Plan(plan)
                            }
                            viewModel.setSubscriptionUiModelList(uiModelList)
                            viewModel.setLoading(LoadType.REFRESH, LoadState.NotLoading.Complete)
                        } else {
                            val responseCode = productDetailsResult.billingResult.responseCode
                            val t = EmptyInAppProductsException("Cannot get plans from Billing Client code = $responseCode")
                            Timber.v(t)
                            viewModel.setLoading(LoadType.REFRESH, LoadState.Error(t))
                            viewModel.setError(t, UiText.DynamicString("Failed to fetch plans. Code: 1000"))
                            ifEnvDev {
                                viewModel.setSubscriptionUiModelList(
                                    internalPlans.map { plan ->
                                        if (plan.bestSeller) {
                                            uiAction(SubscriptionUiAction.ToggleSelectedPlan(plan.id))
                                        }
                                        SubscriptionUiModel.Plan(plan)
                                    }
                                )
                                viewModel.setLoading(LoadType.REFRESH, LoadState.NotLoading.Complete)
                            }
                        }
                    }
                } else {
                    Timber.d("Billing client is not ready.")
                    /*val t = EmptyInAppProductsException("Cannot get plans from Billing Client code = -1")
                    Timber.v(t)
                    viewModel.setLoading(LoadType.REFRESH, LoadState.Error(t))
                    viewModel.setError(t, UiText.DynamicString("Failed to fetch plans. Code: 1000"))
                    ifEnvDev {
                        internalPlans?.map { plan ->
                            if (plan.bestSeller) {
                                uiAction(SubscriptionUiAction.ToggleSelectedPlan(plan.id))
                            }
                            SubscriptionUiModel.Plan(plan)
                        }?.let { uiModelList -> viewModel.setSubscriptionUiModelList(uiModelList) }
                        viewModel.setLoading(LoadType.REFRESH, LoadState.NotLoading.Complete)
                    }*/
                }
            }
        }

        val uiModelListNotEmptyFlow = uiState.map { it.subscriptionPlansUiModels }
            .map { it.isNotEmpty() }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                billingConnectionStateFlow,
                viewModel.pendingPurchaseSignal,
                uiModelListNotEmptyFlow,
                ::Triple
            ).collectLatest { (connected, signal, notEmpty) ->
                if (connected && notEmpty) {
                    checkPendingPurchases()
                }
            }
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
                        viewModel.setLoginUser(loginUser)
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

    private fun gotoClaimCoupon() {
        safeCall {
            findNavController().apply {
                navigate(R.id.action_subscription_plans_to_claim_coupon)
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

    private fun confirmPaymentMethod(
        uiAction: (SubscriptionUiAction) -> Unit
    ) {
        val paymentMethodModes = arrayListOf(
            PaymentMethodData(
                paymentMethod = PaymentMethod.IN_APP,
                title = "Google Play",
                description = "Complete your purchase with Google Play.",
                brandLogo = R.drawable.ic_brand_play_store
            )
        )
        ifEnvDev {
            paymentMethodModes.add(
                PaymentMethodData(
                    paymentMethod = PaymentMethod.OTHER,
                    title = "Test",
                    description = "Bypass the payment."
                )
            )
        }

        PaymentMethodSheet(
            paymentMethodModes
        ) { data ->
            when (data.paymentMethod) {
                PaymentMethod.IN_APP -> {
                    lifecycleScope.launch {
                        // TODO: launch in-app purchase flow
                        viewModel.getSelectedPlan()?.let { selectedPlan ->
                            val txnId = viewModel.initPayment(selectedPlan)
                            this@SubscriptionFragment.transactionId = txnId

                            Intent(requireActivity(), InAppPurchaseActivity::class.java).apply {
                                putExtra(InAppPurchaseActivity.EXTRA_PRODUCT_SKU, selectedPlan.productId)
                                putExtra(InAppPurchaseActivity.EXTRA_TRANSACTION_ID, txnId)
                                inAppPurchaseResultLauncher.launch(this)
                            }
                        }.ifNull {
                            withContext(Dispatchers.Main) {
                                context?.showToast("Cannot complete your purchase right now")
                            }
                        }
                    }
                }
                PaymentMethod.OTHER -> {
                    uiAction(SubscriptionUiAction.NextClick)
                }
            }
        }.also {
            it.show(childFragmentManager, PaymentMethodSheet.TAG)
        }
    }

    /* Play Billing */
    private fun initBillingClientIfRequired() {
       if (!isBillingClientConnected) {
           billingClient.startConnection(object : BillingClientStateListener {
               override fun onBillingSetupFinished(p0: BillingResult) {
                   isBillingClientConnected = true
                   viewModel.setBillingConnectionState(isBillingClientConnected)
                   val msg = "Billing client setup finished"
                   Timber.v(msg)
                   Timber.d("code = ${p0.responseCode} ${p0.debugMessage} ")
               }

               override fun onBillingServiceDisconnected() {
                   isBillingClientConnected = false
                   viewModel.setBillingConnectionState(isBillingClientConnected)
                   val msg = "Billing client disconnected"
                   val t = IllegalStateException(msg)
                   Timber.w(t)
               }
           })
       }
    }

    private fun checkPendingPurchases() {
        if (pendingPurchaseFetchJob?.isActive == true) {
            val t = IllegalStateException("A pending purchase fetch job is already active. Ignoring request.")
            ifDebug { Timber.e(t) }
            return
        }
        pendingPurchaseFetchJob?.cancel(CancellationException("New request")) // just in case
        pendingPurchaseFetchJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            kotlin.runCatching { billingClient.queryPurchases(ProductType.INAPP) }
                .onSuccess { purchases ->
                    val _p = purchases.map {
                        immutableListOf(it.products.joinToString(), it.purchaseState, it.signature)
                    }
                        .joinToString()
                    Timber.d("Purchases: $_p")

                    // TODO: handle pending purchase
                    purchases.filter { it.purchaseState == PurchaseState.PURCHASED }
                        .onEach { purchase ->
                        viewModel.getPlanDetailForProductId(purchase.products.first())?.let { planDetail ->
                            Timber.d("Purchases: planDetail = $planDetail")
                            billingClient.queryProductDetails(
                                QueryProductDetailsParams.newBuilder()
                                    .setProductList(
                                        purchase.products.map { productId ->
                                            QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId(productId)
                                                .setProductType(ProductType.INAPP)
                                                .build()
                                        }
                                    )
                                    .build()
                            ).productDetailsList?.first()?.let { productDetails ->
                                Timber.d("Purchases: details $productDetails")
                                showPendingPurchaseDialog(productDetails, planDetail)
                            }
                        }
                    }
                }
                .onFailure { t ->
                    Timber.e(t)
                }
            // checkPurchaseHistory()
        }
    }

    private fun checkPurchaseHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            billingClient.queryPurchaseHistory(
                QueryPurchaseHistoryParams.newBuilder()
                    .setProductType(ProductType.INAPP)
                    .build()
            ).let { purchaseHistoryResult ->
                val (billingClientResponse, record) = purchaseHistoryResult.billingResult to
                        purchaseHistoryResult.purchaseHistoryRecordList
                when (billingClientResponse.responseCode) {
                    BillingResponseCode.OK -> {
                        if (record != null) {
                            Timber.d("Products History: $record")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    /* END - Play Billing */

    private fun showPendingPurchaseDialog(productDetails: ProductDetails, subscriptionPlan: SubscriptionPlan) {
        SimpleDialog(
            context = requireContext(),
            titleText = "Purchase",
            message = "You have already purchased a product '${productDetails.name}'. Would you like to proceed? " +
                    "If you cancel any amount deducted will be refunded to your account.",
            positiveButtonText = "Proceed",
            positiveButtonAction = {
                // TODO: consume the product
            },
            negativeButtonText = "Cancel",
            negativeButtonAction = {
                // TODO: handle canceled pending purchase
            },
            cancellable = false,
            showCancelButton = false
        ).also {
            it.show()
            pendingDialogWeakRef = it
        }
    }

    override fun onResume() {
        super.onResume()

        if (isBillingClientConnected) {
            viewModel.setPendingPurchaseSignal()
        }
    }
}