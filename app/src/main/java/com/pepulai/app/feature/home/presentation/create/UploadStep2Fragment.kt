package com.pepulai.app.feature.home.presentation.create

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pepulai.app.R
import com.pepulai.app.commons.presentation.dialog.SimpleDialog
import com.pepulai.app.databinding.FragmentUploadStep2Binding

class UploadStep2Fragment : Fragment() {

    private var isSettingsLaunched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_upload_step2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentUploadStep2Binding.bind(view)

        binding.bindState()
    }

    private fun FragmentUploadStep2Binding.bindState() {

        SimpleDialog(
            context = requireContext(),
            popupIcon = R.drawable.ic_files_permission,
            titleText = getString(R.string.permissions_required),
            message = getString(R.string.files_permission_des),
            positiveButtonText = "Settings",
            positiveButtonAction = {  /* go to settings */  openSettings() },
            cancellable = true,
            showCancelButton = true
        ).show()

        btnNext.setOnClickListener {
            try {
                findNavController().apply {
                    navigate(R.id.action_upload_step_2_to_upload_step_3)
                }
            } catch (ignore: Exception) {}
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
        isSettingsLaunched = true
    }

    override fun onResume() {
        super.onResume()
        if (isSettingsLaunched) {
            // gotoCamera()
            isSettingsLaunched = false
        }
    }
}