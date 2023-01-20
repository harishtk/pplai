package com.aiavatar.app.feature.home.presentation.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentUploadStep1Binding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.onboard.presentation.walkthrough.SquareImageAdapter

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

        val adapter = SquareImageAdapter()
        goodExamplesList.adapter = adapter

        val resList = listOf<Int>(
            R.drawable.wt_small_grid_1,
            R.drawable.wt_small_grid_2,
            R.drawable.wt_small_grid_3,
            R.drawable.wt_small_grid_4,
            R.drawable.wt_small_grid_5,
            R.drawable.wt_small_grid_6,
            R.drawable.wt_small_grid_7,
            R.drawable.wt_small_grid_8,
            R.drawable.wt_small_grid_9,
            R.drawable.wt_small_grid_10,
            R.drawable.wt_small_grid_11,
            R.drawable.wt_small_grid_12,
        )
        adapter.submitList(resList)

        btnNext.setOnClickListener {
            try {
                findNavController().apply {
                    navigate(R.id.action_upload_step_1_to_upload_step_2)
                }
            } catch (ignore: Exception) {}
        }

        tvSkip.setOnClickListener {
            findNavController().apply {
                val navOpts = NavOptions.Builder()
                    .setPopUpTo(R.id.upload_step_1, inclusive = true, saveState = false)
                    .setEnterAnim(R.anim.slide_in_left)
                    .setExitAnim(R.anim.slide_out_left)
                    .build()
                navigate(R.id.landingPage, null, navOpts)
            }
        }
        tvSkip.isVisible = !ApplicationDependencies.getPersistentStore().isLogged
    }
}

