/*
package com.aiavatar.app.pay.billing

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pepul.socialnetworking.databinding.ActivityInAppBillingBinding
import com.pepul.socialnetworking.model.LogHelper
import com.pepul.socialnetworking.mvvm.local.session.SessionPref
import com.pepul.socialnetworking.mvvm.remote.Constant
import com.pepul.socialnetworking.mvvm.ui.activity.creators.ui.activity.SecondaryBillingActivity
import com.pepul.socialnetworking.mvvm.ui.activity.creatorsprofile.ui.fragments.GetCoinsBottomSheet
import com.pepul.socialnetworking.mvvm.utils.AnalyticsEvents
import com.pepul.socialnetworking.mvvm.utils.EventBusDataShareModel.DataShareAppModel
import com.pepul.socialnetworking.mvvm.utils.InAppBillingHelper
import com.pepul.socialnetworking.mvvm.viewmodel.RewardViewModel
import com.pepul.socialnetworking.pepul_creators.creators_analytics.firebase.CreatorsFirebaseEvent
import com.pepul.socialnetworking.pepul_creators.model.LoggingEventDataModel
import com.pepul.socialnetworking.pepul_creators.ui.activity.CreatorsHomeActivity
import com.pepul.socialnetworking.pepul_creators.utils.CountDownTimer
import com.pepul.socialnetworking.pepul_creators.utils.CustomDialog.CommonDialogHelper
import com.pepul.socialnetworking.pepul_creators.utils.Interface.CreatorsDialogNegativeButtonClick
import com.pepul.socialnetworking.pepul_creators.utils.Interface.CreatorsDialogPositiveButtonClick
import com.pepul.socialnetworking.pepul_creators.utils.constants.common_constants.CommonConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject


internal const val PURCHASE_ID: String = "purchaseId"
internal const val TITLE: String = "title"
internal const val STORE_ID: String = "storeId"
internal const val CREATOR_ID: String = "creatorId"
internal const val PROMOTION_TOKEN: String = "promotionToken"

@DelicateCoroutinesApi
@AndroidEntryPoint
class InAppBillingActivity : AppCompatActivity(), InAppBillingHelper.InAppBillingListener,
    GetCoinsBottomSheet.Callback {

    //    initial initialization
    private lateinit var timer: CountDownTimer
    private var purchaseId: Int = -1
    private var title: String = ""
    private var storeId: String = ""
    private var promotionToken: String = ""
    private var creatorId: String = "0"
    private val progressTime: Long = 3000L
    private val backPressTime: Long = 3000L
    private var isBackPressed: Boolean = false


    private val rewardsViewModel by viewModels<RewardViewModel>()
    private val binding by lazy { ActivityInAppBillingBinding.inflate(layoutInflater) }

    private lateinit var inAppBillingHelper: InAppBillingHelper

    @Inject
    lateinit var sessionPref: SessionPref

    @Inject
    lateinit var firebaseEvent: CreatorsFirebaseEvent

    @Inject
    lateinit var logHelper: LogHelper

    external fun getKey3(): String
    external fun getKey4(): String

    //    1 = In App Plan Launcher
    //    2 = Web View Launcher
    private var defaultPlanLauncher = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


//        lifecycleScope.launch(Dispatchers.IO) {
//            logHelper.addApplicationLog(screenName = LogHelper.Screens.PAYMENT_SCREEN, eventType = LogHelper.EventType.INSTANT, eventName = LogHelper.EventName.SCREEN_ENTERED)
//        }


        intent?.extras?.let {
            purchaseId = it.getInt(PURCHASE_ID, -1)
            title = it.getString(TITLE, "")
            storeId = it.getString(STORE_ID, "")
            creatorId = it.getString(CREATOR_ID, "0")
        }



        binding.progressBar.max = progressTime.toInt()
        inAppBillingHelper = InAppBillingHelper.newBuilder(
                activity = this@InAppBillingActivity, scope = lifecycleScope, rewardViewModel = rewardsViewModel, sessionPref = sessionPref, callback = this@InAppBillingActivity
        ).apply {
            enableBilling()
        }

        binding.imageButtonClose.setOnClickListener {
            onBackPressed()
            runBlocking(Dispatchers.IO) {
                logHelper.addApplicationLog(
                        screenName = LogHelper.Screens.PAYMENT_SCREEN,
                        eventType = LogHelper.EventType.INSTANT,
                        eventName = LogHelper.EventName.Payment.COINS_PACKAGE_CANCELLED
                )
            }
        }

        timer = CountDownTimer.getInstance().apply {
            setCountDownTime(progressTime)
        }

        if (title.isNotEmpty() && purchaseId != -1 && storeId.isNotEmpty()) {
            startInAppBillingScreen()
        } else {

            defaultPlanLauncher = try {
                sessionPref.getPackageLauncher()
            } catch (e: Exception) {
                Timber.e(e)
                1
            }

            when (defaultPlanLauncher) {
                1 -> inAppPlanLauncher()
                2 -> webViewPlanLauncher()
                else -> inAppPlanLauncher() // Default is In App Billing
            }

        }

        AnalyticsEvents.adjustEvent(Constant.creator_payment_page_visitor)

    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventType = LogHelper.EventType.PAGE,
                    eventName = LogHelper.EventName.SCREEN_MAXIMIZE
            )
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventType = LogHelper.EventType.PAGE,
                    eventName = LogHelper.EventName.SCREEN_MINIMIZE
            )
        }
    }

    private fun inAppPlanLauncher() {
        binding.imageButtonClose.isVisible = false
        binding.imageViewPepulLogo.isVisible = false
        GetCoinsBottomSheet.getInstance(callback = this@InAppBillingActivity).apply {
            isCancelable = false
            show(supportFragmentManager, GetCoinsBottomSheet::class.java.canonicalName)
        }
    }

    private fun webViewPlanLauncher() {
        initializeWebViewPackageView()


        runBlocking(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventType = LogHelper.EventType.INSTANT,
                    eventName = LogHelper.EventName.Payment.PURCHASE_COINS_PAGE_OPENED
            )
        }


        binding.apply {
            imageButtonClose.isVisible = true
            imageViewPepulLogo.isVisible = true
            webView.apply {
                settings.javaScriptEnabled = false
                settings.setSupportZoom(false)
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        setWebViewProgressBar(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        setWebViewProgressBar(false)
                        if (!url!!.toString().contains("welcome", true)) {
                            runBlocking(Dispatchers.IO) {
                                logHelper.addApplicationLog(
                                        screenName = LogHelper.Screens.PAYMENT_SCREEN,
                                        eventType = LogHelper.EventType.INSTANT,
                                        eventName = LogHelper.EventName.Payment.PURCHASE_COINS_PAGE_LOADED
                                )
                            }
                            binding.webView.isVisible = true
                        } else {
                            binding.webView.isVisible = false
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        setWebViewProgressBar(false)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return if (!request!!.url.host.toString().contains(getKey4(), true)) {
                            hideWebView()
                            false
                        } else {
                            if (request.url.toString().contains("welcome", true)) {
                                this@InAppBillingActivity.purchaseId =
                                    request.url.getQueryParameter(PURCHASE_ID).toString().toInt()
                                this@InAppBillingActivity.title =
                                    request.url.getQueryParameter(TITLE).toString()
                                this@InAppBillingActivity.storeId =
                                    request.url.getQueryParameter(STORE_ID).toString()
                                startInAppBillingScreen()
                            }
                            false
                        }
                    }
                }
                loadUrl(getKey3())
                triggerScreenVisitFirebaseEvent()
            }
        }
    }

    private fun hideWebView() {
        binding.apply {
            webView.isVisible = false
            imageButtonClose.isVisible = false
            imageViewPepulLogo.isVisible = false
        }
    }

    private fun startInAppBillingScreen() {

        runBlocking(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventType = LogHelper.EventType.INSTANT,
                    eventName = LogHelper.EventName.Payment.COINS_PACKAGE_CLICK,
                    additionalData = Gson().toJson(JsonObject().apply {
                        addProperty("purchaseId", purchaseId)
                    })
            )
        }


        rewardsViewModel.logEvent(LoggingEventDataModel(logContent = "startInAppBillingScreen"))
        initializeInAppBillingView()

        timer.startTimer(object : CountDownTimer.Callback {
            override fun onStart() {

            }

            override fun onTick(remainingTimer: Long) {
                Timber.i("remainingTimer >> $remainingTimer")
                lifecycleScope.launch(Dispatchers.Main) {
                    if (remainingTimer != 0L) {
                        binding.textViewPayment.text =
                            "Payment will start in ${remainingTimer / 1000}"
                    } else {
                        binding.textViewPayment.text = "Payment will start now"
                    }
                    binding.progressBar.setProgress((progressTime - remainingTimer).toInt(), true)
                }
            }

            override fun onCompleted() {
                Timber.i("remainingTimer >> Completed")

                lifecycleScope.launch(Dispatchers.Main) {
                    binding.textViewPayment.isVisible = false
                    binding.progressBar.isVisible = false

                    // Start Billing
                    inAppBillingHelper.startBilling(purchaseId = purchaseId, title = title, storeId = storeId, promotionToken = promotionToken, creatorId = creatorId)

                    triggerPlanChooseFirebaseEvent(title)
                }

            }

            override fun onCancelled(pendingTimer: Long) {
                Timber.i("Count down stopped")
            }
        })
    }


    private fun initializeWebViewPackageView() {
        binding.apply {
            textViewPayment.isVisible = false
            progressBar.isVisible = false
            linearLayoutPaymentNote.isVisible = false

            webView.isVisible = false

            imageViewPepulLogo.isVisible = true
            imageButtonClose.isVisible = true
            progressBar2.isVisible = true
        }

    }

    private fun initializeInAppBillingView() {
        binding.apply {
            textViewPayment.isVisible = true
            progressBar.isVisible = true
            linearLayoutPaymentNote.isVisible = true

            webView.isVisible = false
            imageViewPepulLogo.isVisible = false
            imageButtonClose.isVisible = false
            progressBar2.isVisible = false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (timer.isCounting) {
            timer.cancelTimer()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventName = LogHelper.EventName.SCREEN_CLOSED,
                    eventType = LogHelper.EventType.PAGE
            )
        }

    }

    override fun onBackPressed() {
        if (!isBackPressed) {
            Toast.makeText(
                this@InAppBillingActivity,
                "Press again to cancel payment",
                Toast.LENGTH_LONG
            ).show()
            isBackPressed = true
            CountDownTimer.getInstance().apply {
                setCountDownTime(backPressTime)
                startTimer(object : CountDownTimer.Callback {

                    override fun onStart() {

                    }

                    override fun onTick(remainingTimer: Long) {
                    }

                    override fun onCompleted() {
                        isBackPressed = false
                    }

                    override fun onCancelled(pendingTimer: Long) {
                    }
                })
            }
        } else {
            finish()
        }
    }

    //    All interface callback here
    override fun onBillingCompleted(creatorId: String) {

        runBlocking(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventType = LogHelper.EventType.INSTANT,
                    eventName = LogHelper.EventName.Payment.PURCHASE_COMPLETED,
                    additionalData = Gson().toJson(JsonObject().apply {
                        addProperty("purchaseId", purchaseId)
                    })
            )
        }


        lifecycleScope.launch(Dispatchers.Main) {
            binding.progressBar.isVisible = false
            CommonDialogHelper(
                mContext = this@InAppBillingActivity,
                positiveButtonClick = object : CreatorsDialogPositiveButtonClick {
                    override fun onItemClickListener(listener: View.OnClickListener) {
                        startActivity(
                            Intent(
                                this@InAppBillingActivity,
                                CreatorsHomeActivity::class.java
                            ).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra(CommonConstants.SHOW_POPULAR_CREATORS, true)
                            })
                        finish()
                    }
                },
                negativeButtonClick = object : CreatorsDialogNegativeButtonClick {
                    override fun onCheckedChangedListener(isChecked: Boolean) {
                    }

                    override fun onSpanTextClickedListener() {
                        startActivity(
                            Intent(
                                this@InAppBillingActivity,
                                CreatorsHomeActivity::class.java
                            ).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra(CommonConstants.SHOW_POPULAR_CREATORS, false)
                            })
                        finish()
                    }

                    override fun onNegativeButtonClicked() {

                    }
                }).showPaymentSuccessDialog(this@InAppBillingActivity)
            firebaseEvent.triggerPaymentSuccessEvent(title)
        }



        try {
            val model = DataShareAppModel()
            model.setAction(CommonConstants.PaymentEvents.EVENT_ON_PAYMENT_SUCCESS)
            EventBus.getDefault().post(model)



            if (creatorId.isNotEmpty() && creatorId.toInt() != 0) {
                //            Trigger if creator id is available for auto subscribe

                println("creatorID>>$creatorId")

                val creatorsEvent = DataShareAppModel().apply {
                    setAction(CommonConstants.UPDATE_VIDEO_DETAILS)
                    setCreatorId(creatorId.toLong())
                    setTriggerCoinApi(true)
                    setSubscribeStatus(true)
                }
                EventBus.getDefault().postSticky(creatorsEvent)
            }


        } catch (e: Exception) {
            Timber.i("InAppBilling >> on payment success")
        }

    }

    override fun onBillingError() {
        runBlocking(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventType = LogHelper.EventType.INSTANT,
                    eventName = LogHelper.EventName.Payment.PAYMENT_ERROR,
                    additionalData = Gson().toJson(JsonObject().apply {
                        addProperty("purchaseId", purchaseId)
                    })
            )
        }

        lifecycleScope.launch(Dispatchers.Main) {
            CommonDialogHelper(mContext = this@InAppBillingActivity, positiveButtonClick = object : CreatorsDialogPositiveButtonClick {
                override fun onItemClickListener(listener: View.OnClickListener) {
                    inAppBillingHelper.startBilling(purchaseId = purchaseId, title = title, storeId = storeId, promotionToken = promotionToken, creatorId = creatorId)
                }
            }, negativeButtonClick = object : CreatorsDialogNegativeButtonClick {

                    override fun onCheckedChangedListener(isChecked: Boolean) {
                        finish()
                    }

                    override fun onSpanTextClickedListener() {
                        finish()
                    }

                override fun onNegativeButtonClicked() {

                }

            }).showPaymentFailureDialog(this@InAppBillingActivity)
        }

        try {
            val model = DataShareAppModel()
            model.setAction(CommonConstants.PaymentEvents.EVENT_ON_PAYMENT_FAILED)
            EventBus.getDefault().post(model)
        } catch (e: Exception) {
            Timber.i("InAppBilling >> on payment failed")
        }
        firebaseEvent.triggerPaymentFailedEvent(title)
    }

    override fun onBillingFailed() {
        runBlocking(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventType = LogHelper.EventType.INSTANT,
                    eventName = LogHelper.EventName.Payment.PAYMENT_FAILED,
                    additionalData = Gson().toJson(JsonObject().apply {
                        addProperty("purchaseId", purchaseId)
                    })
            )
        }

        lifecycleScope.launch(Dispatchers.Main) {
            CommonDialogHelper(mContext = this@InAppBillingActivity, positiveButtonClick = object : CreatorsDialogPositiveButtonClick {
                override fun onItemClickListener(listener: View.OnClickListener) {
                    startInAppBillingScreen()
                }
            }, negativeButtonClick = object : CreatorsDialogNegativeButtonClick {
                override fun onCheckedChangedListener(isChecked: Boolean) {
                    finish()
                }

                    override fun onSpanTextClickedListener() {
                        finish()
                    }

                override fun onNegativeButtonClicked() {

                }

            }).showPaymentFailureDialog(this@InAppBillingActivity)
        }

        try {
            val model = DataShareAppModel()
            model.setAction(CommonConstants.PaymentEvents.EVENT_ON_PAYMENT_FAILED)
            EventBus.getDefault().post(model)
        } catch (e: Exception) {
            Timber.i("InAppBilling >> on payment failed")
        }
        firebaseEvent.triggerPaymentFailedEvent(title)
    }

    override fun onUserRejected() {
        runBlocking(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventType = LogHelper.EventType.INSTANT,
                    eventName = LogHelper.EventName.Payment.PAYMENT_CANCELLED,
                    additionalData = Gson().toJson(JsonObject().apply {
                        addProperty("purchaseId", purchaseId)
                    })
            )
        }

        lifecycleScope.launch(Dispatchers.Main) {
            CommonDialogHelper(mContext = this@InAppBillingActivity, positiveButtonClick = object : CreatorsDialogPositiveButtonClick {
                override fun onItemClickListener(listener: View.OnClickListener) {
                    startInAppBillingScreen()
                }
            }, negativeButtonClick = object : CreatorsDialogNegativeButtonClick {

                    override fun onCheckedChangedListener(isChecked: Boolean) {
                        finish()
                    }

                    override fun onSpanTextClickedListener() {
                        finish()
                    }

                override fun onNegativeButtonClicked() {

                }

            }).showPaymentUserRejectedDialog(this@InAppBillingActivity)
        }

        try {
            val model = DataShareAppModel()
            model.setAction(CommonConstants.PaymentEvents.EVENT_ON_PAYMENT_REJECTED)
            EventBus.getDefault().post(model)
        } catch (e: Exception) {
            Timber.i("InAppBilling >> on payment user rejected")
        }
    }

    override fun onBillingProcessing() {
        runBlocking(Dispatchers.IO) {
            logHelper.addApplicationLog(
                    screenName = LogHelper.Screens.PAYMENT_SCREEN,
                    eventType = LogHelper.EventType.INSTANT,
                    eventName = LogHelper.EventName.Payment.PAYMENT_SUCCESS,
                    additionalData = Gson().toJson(JsonObject().apply {
                        addProperty("purchaseId", purchaseId)
                    })
            )
        }

        lifecycleScope.launch(Dispatchers.Main) {
            binding.textViewPayment.apply {
                isVisible = true
                text = "Verifying payment, please wait..."
            }
            binding.progressBar.isIndeterminate = true
            binding.progressBar.isVisible = true
        }
    }

    override fun onPaymentTypeSelect(
        paymentType: Int,
        purchaseId: Int,
        title: String,
        storeId: String,
        promotionToken: String
    ) {
        if (paymentType == 1) {

            this@InAppBillingActivity.purchaseId = purchaseId
            this@InAppBillingActivity.title = title
            this@InAppBillingActivity.storeId = storeId
            this@InAppBillingActivity.promotionToken = promotionToken

            startInAppBillingScreen()
        } else {

            startActivity(
                    Intent(
                            this@InAppBillingActivity, SecondaryBillingActivity::class.java
                    ).apply {
                        putExtra(CommonConstants.NavigationKey.PAYMENT_KEY, purchaseId)
                    })
        }
    }

    fun setWebViewProgressBar(isLoading: Boolean) {
        binding.progressBar2.isVisible = isLoading
    }

    override fun onClose() {
        finish()
    }

    private fun triggerScreenVisitFirebaseEvent() {
        firebaseEvent.triggerPackageScreenVisitEvent()
    }

    private fun triggerPlanChooseFirebaseEvent(selectedPlan: String) {
        firebaseEvent.triggerPlanChooseClickEvent(selectedPlan)
    }
}*/
