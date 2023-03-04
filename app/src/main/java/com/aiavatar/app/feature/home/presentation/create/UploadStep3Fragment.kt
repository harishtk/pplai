package com.aiavatar.app.feature.home.presentation.create

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.*
import com.aiavatar.app.analytics.Analytics
import com.aiavatar.app.analytics.AnalyticsLogger
import com.aiavatar.app.viewmodels.SharedViewModel
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.cancelSpinning
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.net.isConnected
import com.aiavatar.app.commons.util.setSpinning
import com.aiavatar.app.databinding.FragmentUploadStep3Binding
import com.aiavatar.app.databinding.ItemGenderSelectableBinding
import com.aiavatar.app.feature.home.presentation.create.util.CreateCreditExhaustedException
import com.aiavatar.app.feature.home.presentation.util.GenderModel
import com.aiavatar.app.feature.onboard.domain.model.CreateCheckData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UploadStep3Fragment : Fragment() {

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    private val viewModel: UploadStep3ViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val createSharedViewModel: CreateSharedViewModel by activityViewModels()

    private var sessionIdCache: Long? = null
    private var maxModelsReachedDialogWeakRef: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_upload_step3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentUploadStep3Binding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )

        setupObservers()
    }

    private fun FragmentUploadStep3Binding.bindState(
        uiState: StateFlow<Step3State>,
        uiAction: (Step3UiAction) -> Unit,
        uiEvent: SharedFlow<Step3UiEvent>,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is Step3UiEvent.NextScreen -> {
                        // TODO: don't user work manager for simplicity sake!
                        if (sessionIdCache != null) {
                            // WorkUtil.scheduleUploadWorker(requireContext(), sessionIdCache!!)
                        }
                        gotoNextScreen(sessionIdCache)
                    }
                }
            }
        }
        val adapter = GenderAdapter { position ->
            viewModel.toggleSelection(position)
            analyticsLogger.logEvent(Analytics.Event.UPLOAD_STEP_3_GENDER_TOGGLE)
        }

        genderSelectionList.adapter = adapter

        val genderModelListFlow = uiState.map { it.genderList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            genderModelListFlow.collectLatest { genderModelList ->
                adapter.submitList(genderModelList)
            }
        }

        val sessionStatusFlow = uiState.map { it.sessionStatus }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            sessionStatusFlow.collectLatest { sessionStatus ->
                Timber.d("Session status: $sessionStatus")
            }
        }

        val loadStateFlow = uiState.map { it.loadState }
            .distinctUntilChanged { old, new ->
                old.refresh == new.refresh && old.action == new.action
            }
        viewLifecycleOwner.lifecycleScope.launch {
            loadStateFlow.collectLatest { loadState ->
                if (loadState.action is LoadState.Loading) {
                    btnNext.setSpinning()
                } else {
                    btnNext.cancelSpinning()
                }
            }
        }

        val notLoadingFlow = loadStateFlow
            .map {
                it.refresh !is LoadState.Loading && it.action !is LoadState.Loading
            }
        val hasErrorsFlow = uiState.map { it.exception != null }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                notLoadingFlow,
                hasErrorsFlow,
                Boolean::and
            ).collectLatest {
                if (it) {
                    val (e, uiErr) = uiState.value.exception to
                            uiState.value.uiErrorText
                    if (e != null) {
                        when (e) {
                            is CreateCreditExhaustedException -> {
                                // Noop
                            }
                        }
                        uiAction(Step3UiAction.ErrorShown(e))
                    }
                }
            }
        }

        bindClick()

    }

    private fun FragmentUploadStep3Binding.bindClick() {
        btnNext.setOnClickListener {
            // TODO: check if network is available
            if (context?.isConnected() != true) {
                context?.showToast(UiText.noInternet.asString(requireContext()))
            } else {
                viewModel.checkCreate { result: Result<CreateCheckData> ->
                    result.onSuccess {
                        if (sessionIdCache != null) {
                            btnNext.setOnClickListener(null)
                            viewModel.updateTrainingType(sessionIdCache!!)
                            analyticsLogger.logEvent(Analytics.Event.UPLOAD_STEP_3_NEXT_CLICK)
                        } else {
                            context?.showToast(UiText.somethingWentWrong.asString(requireContext()))
                        }
                    }
                        .onFailure { t ->
                            if (t is CreateCreditExhaustedException) {
                                if (maxModelsReachedDialogWeakRef?.isShowing != true) {
                                    showMaxModelsReachedAlert {
                                        gotoProfile()
                                    }
                                } else {
                                    maxModelsReachedDialogWeakRef?.dismiss()
                                }
                            } else {
                                context?.showToast(UiText.somethingWentWrong.asString(requireContext()))
                            }
                        }
                }
            }
        }

        navigationIcon.setOnClickListener {
            safeCall { findNavController().popBackStack() }
            analyticsLogger.logEvent(Analytics.Event.UPLOAD_STEP_3_NAVIGATION_BACK_CLICK)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.currentUploadSessionId.collectLatest { sessionId ->
                Timber.d("Session id: $sessionId")
                if (sessionId != null) {
                    sessionIdCache = sessionId
                    viewModel.setSessionId(sessionId)
                }
            }
        }
    }

    private fun gotoNextScreen(sessionId: Long?) = safeCall {
        Log.d("UploadStep3Fragment", "gotoNextScreen() called")
        findNavController().apply {
            val navOpts = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_right)
                .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                .build()
            val args = Bundle().apply {
                if (sessionId != null) {
                    putLong(Constant.ARG_UPLOAD_SESSION_ID, sessionId)
                }
            }
            navigate(R.id.avatar_status, args, navOpts)
        }
    }

    private fun gotoProfile() {
        safeCall {
            findNavController().apply {
                val navOptions = defaultNavOptsBuilder()
                    // .setPopUpTo(R.id.upload_step_1, inclusive = true)
                    .build()
                navigate(R.id.profile, null, navOptions)
            }
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
            .setMessage("You have exceeded your free limit for model creation. Kindly recreate an existing model " +
                    "to create a new one. (Profile -> Model -> Recreate)")
            .setPositiveButton("GO TO PROFILE", clickListener)
            .setNegativeButton("CANCEL", clickListener)
            .also {
                maxModelsReachedDialogWeakRef = it.show()
            }
    }
}

class GenderAdapter(
    val onToggleSelection: (position: Int) -> Unit
) : ListAdapter<GenderModel, GenderAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_gender_selectable,
            parent,
            false
        )
        val binding = ItemGenderSelectableBinding.bind(itemView)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val model = getItem(position)
        holder.bind(model, onToggleSelection)
    }

    inner class ItemViewHolder(
        private val binding: ItemGenderSelectableBinding,
    ) : ViewHolder(binding.root) {

        fun bind(data: GenderModel, onToggleSelection: (position: Int) -> Unit) = with(binding) {
            checkboxTitle.text = data.title

            toggleSelection(data.selected)
            root.setOnClickListener {
                onToggleSelection(adapterPosition)
            }
        }

        fun toggleSelection(selected: Boolean) = with(binding) {
            root.isSelected = selected
        }
    }

    companion object {
        val DIFF_CALLBACK = object : ItemCallback<GenderModel>() {
            override fun areItemsTheSame(oldItem: GenderModel, newItem: GenderModel): Boolean {
                return oldItem.title == newItem.title
            }

            override fun areContentsTheSame(oldItem: GenderModel, newItem: GenderModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}