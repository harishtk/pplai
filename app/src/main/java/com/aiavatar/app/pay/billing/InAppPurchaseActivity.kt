package com.aiavatar.app.pay.billing

import android.app.Activity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.R

class InAppPurchaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkSecureMode()
        setContentView(R.layout.activity_inapp_purchase)
    }

    private fun checkSecureMode() {
        if (BuildConfig.IS_SECURED) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    companion object {

    }

}