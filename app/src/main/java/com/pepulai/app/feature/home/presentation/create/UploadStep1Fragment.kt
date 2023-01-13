package com.pepulai.app.feature.home.presentation.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pepulai.app.R
import com.pepulai.app.databinding.FragmentUploadStep1Binding

class UploadStep1Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_upload_step1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentUploadStep1Binding.bind(view)

        binding.bindState()
    }

    private fun FragmentUploadStep1Binding.bindState() {
        btnNext.setOnClickListener {
            try {
                findNavController().apply {
                    navigate(R.id.action_upload_step_1_to_upload_step_2)
                }
            } catch (ignore: Exception) {}
        }
    }
}

