package com.aiavatar.app.core.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.Px
import androidx.core.content.res.ResourcesCompat
import com.aiavatar.app.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber
import kotlin.math.abs

abstract class BaseBottomSheetDialogFragment(
    @ColorRes private val backgroundColor: Int
) : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Widget_App_BottomSheetDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        val bgColor = ResourcesCompat.getColor(resources, backgroundColor, null)
        val radius = resources.getDimensionPixelSize(R.dimen.default_bottom_sheet_radius)

        dialog.setOnShowListener {
            val bottomSheetDialog: BottomSheetDialog = it as BottomSheetDialog
            val behavior = bottomSheetDialog.behavior as BottomSheetBehavior<FrameLayout>
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                @SuppressLint("SwitchIntDef")
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Timber.d("BottomSheet.State: $newState")
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            bottomSheet.background = getRoundedDrawable(radius, bgColor)
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val newRadius: Float = if (abs(slideOffset) == 0.0F) {
                        radius.toFloat()
                    } else {
                        radius * (1.0F - abs(slideOffset))
                    }
                    if (bottomSheet.background is GradientDrawable) {
                        (bottomSheet.background as GradientDrawable).cornerRadii = floatArrayOf(
                            radius.toFloat(), radius.toFloat(), // top left
                            radius.toFloat(), radius.toFloat(), // top right
                            0F, 0F, // bottom right
                            0F, 0F  // bottom left
                        )
                    }
                    bottomSheet.background = getRoundedDrawable(newRadius.toInt(), bgColor)
                }
            })

            val bottomSheet: FrameLayout =
                bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                    ?: return@setOnShowListener
            bottomSheet.background = getRoundedDrawable(radius, bgColor)
        }
        return dialog
    }

    private fun getRoundedDrawable(@Px radius: Int, bgColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(bgColor)
            cornerRadii = floatArrayOf(
                radius.toFloat(), radius.toFloat(), // top left
                radius.toFloat(), radius.toFloat(), // top right
                0F, 0F, // bottom right
                0F, 0F  // bottom left
            )
        }
    }
}