package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aiavatar.app.R
import com.aiavatar.app.core.fragment.BaseBottomSheetDialog
import com.aiavatar.app.core.fragment.BaseBottomSheetDialogFragment
import com.aiavatar.app.databinding.DialogLegalsBinding
import com.aiavatar.app.makeLinks
import com.aiavatar.app.showToast

class LegalsBottomSheet(
    val cont: () -> Unit
) : BaseBottomSheetDialogFragment(R.color.bottom_sheet_background) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_legals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogLegalsBinding.bind(view)

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
                context?.showToast("Please read and accept.")
            }
        }
    }

    companion object {
        const val FRAGMENT_TAG = "legals-sheet"
    }
}