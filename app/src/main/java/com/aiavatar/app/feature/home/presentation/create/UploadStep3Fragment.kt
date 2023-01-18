package com.aiavatar.app.feature.home.presentation.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.R
import com.aiavatar.app.SharedViewModel
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.databinding.FragmentUploadStep3Binding
import com.aiavatar.app.databinding.ItemGenderSelectableBinding
import com.aiavatar.app.feature.home.presentation.util.GenderModel
import com.aiavatar.app.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UploadStep3Fragment : Fragment() {

    private val viewModel: UploadStep3ViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var sessionIdCache: Long? = null

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
            uiEvent = viewModel.uiEvent
        )

        setupObservers()
    }

    private fun FragmentUploadStep3Binding.bindState(
        uiState: StateFlow<Step3State>,
        uiEvent: SharedFlow<Step3UiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is Step3UiEvent.NextScreen -> {
                        findNavController().apply {
                            navigate(R.id.avatar_status)
                        }
                    }
                }
            }
        }
        val adapter = GenderAdapter { position ->
            viewModel.toggleSelection(position)
        }

        genderSelectionList.adapter = adapter

        val genderModelListFlow = uiState.map { it.genderList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            genderModelListFlow.collectLatest { genderModelList ->
                adapter.submitList(genderModelList)
            }
        }

        btnNext.setOnClickListener {
            if (sessionIdCache != null) {
                btnNext.setOnClickListener(null)
                viewModel.updateTrainingType(sessionIdCache!!)
            } else {
                context?.showToast(UiText.somethingWentWrong.asString(requireContext()))
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            sharedViewModel.currentUploadSessionId.collectLatest { sessionId ->
                this@UploadStep3Fragment.sessionIdCache = sessionId
            }
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
            root.setOnClickListener { checkbox.isChecked = true }
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    onToggleSelection(adapterPosition)
                }
            }
        }

        fun toggleSelection(selected: Boolean) = with(binding) {
            root.isSelected = selected
            checkbox.isChecked = selected
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