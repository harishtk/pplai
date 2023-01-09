package com.pepul.app.pepulliv.commons.presentation.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.databinding.DialogSimple1Binding

class SimpleDialog constructor(
    context: Context,
    private val message: String,
    @DrawableRes
    private val popupIcon: Int? = null,
    private val titleText: String? = null,
    private val positiveButtonText: String = context.getString(R.string.label_yes),
    private val negativeButtonText: String? = null,
    private val positiveButtonAction: () -> Unit = {},
    private val negativeButtonAction: (() -> Unit)? = null,
    private val cancellable: Boolean = true,
    private val showCancelButton: Boolean = true
) : Dialog(context, R.style.Widget_App_Dialog) {

    private lateinit var binding: DialogSimple1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogSimple1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        setCanceledOnTouchOutside(cancellable)
        setCancelable(cancellable)

        // init views
        with(binding) {
            if (popupIcon != null) {
                bigImage.isVisible = true
                bigImage.setImageResource(popupIcon)
            } else {
                bigImage.isVisible = false
            }

            description.text = message

            if (!titleText.isNullOrBlank()) {
                title.isVisible = true
                title.text = this@SimpleDialog.titleText
            } else {
                title.isVisible = false
            }

            buttonPositive.text = positiveButtonText
            buttonPositive.setOnClickListener {
                dismiss()
                positiveButtonAction()
            }

            if (!negativeButtonText.isNullOrBlank()) {
                buttonNegative.isVisible = true
                buttonNegative.text = negativeButtonText

                buttonNegative.setOnClickListener {
                    dismiss()
                    negativeButtonAction?.invoke()
                }
            } else {
                buttonNegative.isVisible = false
            }

            if (showCancelButton) {
                ivClose.isVisible = true
                ivClose.setOnClickListener { dismiss() }
            } else {
                ivClose.isVisible = false
                ivClose.setOnClickListener(null)
            }
        }

        setOnShowListener {
            with(binding) {
                if (bigImage.isVisible) {
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
                                bigImage.visibility = View.INVISIBLE
                            }

                            override fun onAnimationEnd(p0: Animation?) {
                                bigImage.isVisible = true
                            }

                            override fun onAnimationRepeat(p0: Animation?) {}
                        })
                    }
                    bigImage.startAnimation(scale)
                }
            }

        }
    }
}