package com.pepulai.app.feature.home.presentation.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.pepulai.app.R
import com.pepulai.app.databinding.FragmentUploadStep3Binding
import com.pepulai.app.databinding.ItemGenderSelectableBinding
import com.pepulai.app.feature.home.presentation.util.GenderModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UploadStep3Fragment : Fragment() {

    private val viewModel: UploadStep3ViewModel by viewModels()

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
            uiState = viewModel.uiState
        )
    }

    private fun FragmentUploadStep3Binding.bindState(
        uiState: StateFlow<Step3State>
    ) {
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