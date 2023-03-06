package com.aiavatar.app.feature.home.presentation.create

import android.Manifest
import android.annotation.TargetApi
import android.app.Dialog
import android.content.DialogInterface
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.aiavatar.app.*
import com.aiavatar.app.analytics.Analytics
import com.aiavatar.app.analytics.AnalyticsLogger
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.net.isConnected
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.databinding.FragmentUploadStep1Binding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.onboard.presentation.walkthrough.SquareImageAdapter
import com.aiavatar.app.feature.onboard.presentation.walkthrough.SquareImageItem
import com.aiavatar.app.feature.onboard.presentation.walkthrough.SquareImageUiModel
import com.aiavatar.app.viewmodels.SharedViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UploadStep1Fragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val createSharedViewModel: CreateSharedViewModel by activityViewModels()

    private val viewModel: UploadStep1ViewModel by viewModels()

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {

        } else {
            // TODO: handle rationale if necessary
        }
    }

    private var maxModelsReachedDialogWeakRef: Dialog? = null

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

        binding.bindState(
            uiState = viewModel.uiState
        )
        // setupObservers()

        askNotificationPermission()
    }

    private fun FragmentUploadStep1Binding.bindState(
        uiState: StateFlow<UploadStep1State>
    ) {

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

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChangedBy { it.action }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                Timber.d("Load state: $loadState")
                // btnNext.isEnabled = loadState.action !is LoadState.Loading
                if (loadState.action is LoadState.Loading) {
                    btnNext.setSpinning()
                    // btnNext.fakeDisable()
                } else {
                    btnNext.cancelSpinning()
                    // btnNext.fakeDisable(false)
                }
            }
        }

        btnNext.setOnClickListener {
            analyticsLogger.logEvent(Analytics.Event.UPLOAD_STEP_1_CONTINUE_BTN_CLICK)
            val modelCount = uiState.value.myModelCount
            val authenticationState = userViewModel.authenticationState.value

            if (context?.isConnected() == true) {
                viewModel.checkCreate { result: kotlin.Result<CreateCheckData> ->
                    result
                        .onSuccess {
                            // TODO: handle the fcuking logic!!
                            when {
                                it.siteDown -> {
                                    gotoMaintenance()
                                }
                                !it.allowModelCreate -> {
                                    if (maxModelsReachedDialogWeakRef?.isShowing != true) {
                                        showMaxModelsReachedAlert {
                                            gotoProfile()
                                        }
                                    } else {
                                        maxModelsReachedDialogWeakRef?.dismiss()
                                    }
                                }
                                modelCount > 0 -> {
                                    showModelPickerDialog(
                                        onModelClick = { modelId ->
                                            gotoPlans(modelId)
                                            false
                                        },
                                        onSkip = { gotoUploadStep2() }
                                    )
                                }
                                else -> {
                                    gotoUploadStep2()
                                }
                            }
                        }
                        .onFailure { t ->
                            Timber.w(t, "Unable to check create")
                            gotoUploadStep2()
                            /*if (t is CreateCreditExhaustedException) {
                                if (maxModelsReachedDialogWeakRef?.isShowing != true) {
                                    showMaxModelsReachedAlert {
                                        gotoProfile()
                                    }
                                } else {
                                    maxModelsReachedDialogWeakRef?.dismiss()
                                }
                            } else {
                                context?.showToast(UiText.somethingWentWrong.asString(requireContext()))
                            }*/
                        }
                }
            } else {
                gotoUploadStep2()
            }
        }

        tvSkip.setOnClickListener {
            analyticsLogger.logEvent(Analytics.Event.UPLOAD_STEP_SKIP_CLICK, null)
            findNavController().apply {
                val navOpts = NavOptions.Builder()
                    .setPopUpTo(R.id.upload_step_1, inclusive = false, saveState = false)
                    .setEnterAnim(R.anim.slide_in_left)
                    .setExitAnim(R.anim.slide_out_left)
                    .build()
                navigate(R.id.landingPage, null, navOpts)
            }
        }

        if (ApplicationDependencies.getPersistentStore().isLogged) {
            tvSkip.isVisible = false
            navigationIcon.isVisible = true
        } else {
            tvSkip.isVisible = true
            navigationIcon.isVisible = false
        }

        navigationIcon.setOnClickListener {
            safeCall {
                findNavController().apply {
                    if (!navigateUp()) {
                        if (!popBackStack()) {
                            activity?.finishAffinity()
                        }
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        createSharedViewModel.createCheckDataFlow
            .onEach { result ->
                Timber.d("Check data: $result")
                when (result) {
                    is Result.Loading -> {

                    }
                    is Result.Error -> {
                        viewModel.setLoading(false)
                    }
                    is Result.Success -> {
                        viewModel.setLoading(false)
                        if (result.data != null) {
                            if (!result.data.allowModelCreate) {
                                if (maxModelsReachedDialogWeakRef?.isShowing != true) {
                                    showMaxModelsReachedAlert {
                                        gotoProfile()
                                    }
                                }
                            } else {
                                maxModelsReachedDialogWeakRef?.dismiss()
                            }
                        }
                    }
                }
            }
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun gotoPlans(modelId: String) = safeCall {
        findNavController().apply {
            val navOpts = defaultNavOptsBuilder()
                .setPopUpTo(R.id.login_fragment, inclusive = true, saveState = true)
                .build()
            val args = Bundle().apply {
                putString(Constant.ARG_MODEL_ID, modelId)
            }
            navigate(R.id.subscription_plans, args, navOpts)
        }
    }

    private fun gotoMaintenance() {
        safeCall {
            findNavController().apply {
                val navOptions = defaultNavOptsBuilder()
                    .build()
                navigate(R.id.maintenance, null, navOptions)
            }
        }
    }

    private fun gotoUploadStep2() {
        safeCall {
            findNavController().apply {
                navigate(R.id.action_upload_step_1_to_upload_step_2)
            }
        }
    }

    private fun gotoUploadStep1() {
        try {
            findNavController().apply {
                val navOpts = NavOptions.Builder()
                    .setExitAnim(R.anim.slide_bottom)
                    .setEnterAnim(R.anim.slide_up)
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                    .build()
                navigate(R.id.upload_step_1, null, navOpts)
            }
        } catch (e: Exception) {
            Timber.e(e)
            (activity as? MainActivity)?.restart()
        }
    }

    private fun gotoProfile() {
        safeCall {
            findNavController().apply {
                val navOptions = defaultNavOptsBuilder()
                    .setPopUpTo(R.id.upload_step_1, inclusive = true)
                    .build()
                navigate(R.id.profile, null, navOptions)
            }
        }
    }

    private fun showModelPickerDialog(
        onModelClick: (modelId: String) -> Boolean,
        onSkip: () -> Unit
    ) {
        ModelPickerDialog(
            onModelClick = onModelClick,
            onSkip = onSkip
        ).also {
            it.show(childFragmentManager, "model-picker-dialog")
        }
    }

    private fun showMaxModelsReachedAlert(cont: () -> Unit) {
        val clickListener: DialogInterface.OnClickListener =
            DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        cont()
                    }
                }
                dialog.dismiss()
            }
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialDialog)
            .setTitle("Dear user")
            .setMessage("You have already created your model. To view the model please go to your profile. " +
                    "To create a new model you have to complete your previous model first")
            .setPositiveButton("GO TO PROFILE", clickListener)
            .setNegativeButton("CANCEL", clickListener)
            .also {
                maxModelsReachedDialogWeakRef = it.show()
            }
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

