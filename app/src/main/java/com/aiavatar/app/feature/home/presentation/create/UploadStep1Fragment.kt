package com.aiavatar.app.feature.home.presentation.create

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentUploadStep1Binding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.onboard.presentation.walkthrough.SquareImageAdapter
import com.aiavatar.app.feature.onboard.presentation.walkthrough.SquareImageItem
import com.aiavatar.app.feature.onboard.presentation.walkthrough.SquareImageUiModel
import timber.log.Timber

class UploadStep1Fragment : Fragment() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {

        } else {
            // TODO: handle rationale if necessary
        }
    }

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
        setupObservers()

        askNotificationPermission()
    }

    private fun FragmentUploadStep1Binding.bindState() {

        val adapter = SquareImageAdapter()
        goodExamplesList.adapter = adapter

        /*val resList = listOf<Int>(
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
        )*/

        (goodExamplesList.layoutManager as? GridLayoutManager)?.let { gridLayoutManager ->
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return adapter.getSpanSize(position)
                }
            }
        }

        val header = SquareImageUiModel.Header(
            "What to Expect",
            spanCount = 4
        )
        val description = SquareImageUiModel.Description(
            getString(R.string.upload_step_des1),
            spanCount = 4
        )
        val title = SquareImageUiModel.Header(
            getString(R.string.good_examples),
            spanCount = 4
        )

        val resList = listOf<SquareImageUiModel>(
            header,
            description,
            title,
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_1), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_2), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_3), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_4), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_5), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_6), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_7), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_8), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_9), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_10), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_11), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_12), spanCount = 1),
        )
        adapter.submitList(resList)

        btnNext.setOnClickListener {
            try {
                findNavController().apply {
                    navigate(R.id.action_upload_step_1_to_upload_step_2)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Timber.e(e)
                }
            }
        }

        tvSkip.setOnClickListener {
            findNavController().apply {
                val navOpts = NavOptions.Builder()
                    .setPopUpTo(R.id.upload_step_1, inclusive = false, saveState = false)
                    .setEnterAnim(R.anim.slide_in_left)
                    .setExitAnim(R.anim.slide_out_left)
                    .build()
                navigate(R.id.landingPage, null, navOpts)
            }
        }
        tvSkip.isVisible = !ApplicationDependencies.getPersistentStore().isLogged
    }

    private fun setupObservers() {

    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: (not-important) Check if request rationale should be shown
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

