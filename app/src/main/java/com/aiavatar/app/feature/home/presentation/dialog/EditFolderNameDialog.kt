package com.aiavatar.app.feature.home.presentation.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.AnimationUtil.shakeNow
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.databinding.DialogEditFoldernameBinding

class EditFolderNameDialog(
    private val onSave: (typedName: String) -> String?
) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, R.style.AppTheme_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_edit_foldername, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogEditFoldernameBinding.bind(view)

        binding.bindState()
    }

    private fun DialogEditFoldernameBinding.bindState() {

        btnClose.setOnClickListener { dismissAllowingStateLoss() }

        edFolderName.doAfterTextChanged { editable ->
            val typed = editable.toString()
            inputErrorMessage.isVisible = false
        }

        btnSave.setOnClickListener {
            val typedName = edFolderName.text.toString()
            val result = onSave(typedName)
            if (result != null) {
                inputErrorMessage.isVisible = true
                inputErrorMessage.text = result
                btnSave.shakeNow()
                HapticUtil.createError(requireContext())
            } else {
                dismissAllowingStateLoss()
            }
        }
    }
}