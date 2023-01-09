package com.pepul.app.pepulliv.commons.presentation.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.core.view.isVisible
import com.pepul.app.pepulliv.databinding.DialogNoInternetBinding

class NoInternetDialog(
    context: Context
) : Dialog(context, android.R.style.Theme_Light_NoTitleBar_Fullscreen) {

    private lateinit var binding: DialogNoInternetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogNoInternetBinding.inflate(layoutInflater)

        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT)

        setCancelable(false)
        setCanceledOnTouchOutside(false)

        setContentView(binding.root)

        setOnShowListener {
            with(binding) {
                if (noInternetIcon.isVisible) {
                    val scale = ScaleAnimation(
                        0.0F,
                        1.0F,
                        0.0F,
                        1.0F,
                        ScaleAnimation.RELATIVE_TO_SELF,
                        0.5F,
                        ScaleAnimation.RELATIVE_TO_SELF,
                        0.5F
                    ).apply {
                        startOffset = 150
                        duration =
                            context.resources.getInteger(android.R.integer.config_shortAnimTime)
                                .toLong()
                        interpolator = AccelerateDecelerateInterpolator()
                        setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(p0: Animation?) {
                                noInternetIcon.visibility = View.INVISIBLE
                            }

                            override fun onAnimationEnd(p0: Animation?) {
                                noInternetIcon.isVisible = true
                            }

                            override fun onAnimationRepeat(p0: Animation?) {}
                        })
                    }
                    noInternetIcon.startAnimation(scale)
                }
            }

        }
    }
}