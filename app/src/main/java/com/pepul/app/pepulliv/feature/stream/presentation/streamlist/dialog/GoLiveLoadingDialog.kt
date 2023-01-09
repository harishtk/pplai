package com.pepul.app.pepulliv.feature.stream.presentation.streamlist.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.pepul.app.pepulliv.R

class GoLiveLoadingDialog(
    context: Context
) : Dialog(context, R.style.AppTheme_Dialog_FullScreen) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.requestFeature(Window.FEATURE_NO_TITLE)

        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT)

        setContentView(R.layout.dialog_go_live_loading)

        setCancelable(false)
        setCanceledOnTouchOutside(false)

    }
}