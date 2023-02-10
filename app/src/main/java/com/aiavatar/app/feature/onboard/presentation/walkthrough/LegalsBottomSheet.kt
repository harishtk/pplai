package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.core.fragment.BaseBottomSheetDialogFragment
import com.aiavatar.app.databinding.DialogLegalsBinding
import com.aiavatar.app.makeLinks
import com.aiavatar.app.showToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


class LegalsBottomSheet(
    val cont: () -> Unit,
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
            Unit
        }
        val termsClicked = {
            context?.showToast("Terms")
            Unit
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

        btnNext.setOnClickListener {
            if (cbAcceptTerms.isChecked && cbAcceptPrivacy.isChecked) {
                cont.invoke()
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

    companion object {
        const val FRAGMENT_TAG = "legals-sheet"
    }
}