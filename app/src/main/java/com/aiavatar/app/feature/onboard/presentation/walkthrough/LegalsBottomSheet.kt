package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.AnimationUtil.touchInteractFeedback
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.core.fragment.BaseBottomSheetDialogFragment
import com.aiavatar.app.databinding.DialogLegalsBinding
import com.aiavatar.app.makeLinks
import com.aiavatar.app.showToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


class LegalsBottomSheet(
    val callback: Callback
) : BaseBottomSheetDialogFragment(R.color.bottom_sheet_background) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.dialog_legals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogLegalsBinding.bind(view)

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val dialog = dialog as BottomSheetDialog?
                val bottomSheet = dialog!!.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from<View>(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
            }
        })

        binding.bindState()
    }

    private fun DialogLegalsBinding.bindState() {
        val privacyPolicyString: String = "Privacy Policy"
        val termsAndConditionString: String = "Terms and Conditions"

        tvAcceptTerms.text = "I agree with the $termsAndConditionString"
        tvAcceptPrivacy.text = "I agree with the $privacyPolicyString"
        val privacyClicked = {
            context?.showToast("Privacy policy")
            callback.onReadLegal("privacy")
        }
        val termsClicked = {
            context?.showToast("Terms")
            callback.onReadLegal("terms")
        }

        tvAcceptTerms.makeLinks(
            listOf(
                termsAndConditionString to termsClicked
            )
        )
        tvAcceptPrivacy.makeLinks(
            listOf(
                privacyPolicyString to privacyClicked
            )
        )

        tvTermsReadMore.setOnClickListener {
            it.touchInteractFeedback()
            callback.onReadLegal("terms")
        }
        tvPrivacyReadMore.setOnClickListener {
            it.touchInteractFeedback()
            callback.onReadLegal("privacy")
        }

        btnNext.setOnClickListener {
            if (cbAcceptTerms.isChecked && cbAcceptPrivacy.isChecked) {
                callback.onAcceptClick()
            } else {
                if (!cbAcceptTerms.isChecked) {
                    termsActionContainer.shakeNow()
                }
                if (!cbAcceptPrivacy.isChecked) {
                    privacyActionContainer.shakeNow()
                }
                HapticUtil.createErrorAlt(requireContext())
                // context?.showToast("Please read and accept.")
            }
        }
    }

    interface Callback {
        fun onAcceptClick()
        fun onReadLegal(type: String /* privacy|terms */)
    }

    companion object {
        const val FRAGMENT_TAG = "legals-sheet"
    }
}