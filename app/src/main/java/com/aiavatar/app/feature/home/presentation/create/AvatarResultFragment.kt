package com.aiavatar.app.feature.home.presentation.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.*
import com.aiavatar.app.databinding.FragmentAvatarResultBinding
import com.aiavatar.app.databinding.ItemSquareImageBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.presentation.catalog.ModelDetailFragment
import com.aiavatar.app.feature.home.presentation.dialog.EditFolderNameDialog
import com.aiavatar.app.viewmodels.SharedViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AvatarResultFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val viewModel: AvatarResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApplicationDependencies.getPersistentStore().currentAvatarStatusId?.let {
            viewModel.setAvatarStatusId(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_avatar_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAvatarResultBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState,
            uiAction = viewModel.accept,
            uiEvent = viewModel.uiEvent
        )
    }

    private fun FragmentAvatarResultBinding.bindState(
        uiState: StateFlow<AvatarResultState>,
        uiAction: (AvatarResultUiAction) -> Unit,
        uiEvent: SharedFlow<AvatarResultUiEvent>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            uiEvent.collectLatest { event ->
                when (event) {
                    is AvatarResultUiEvent.ShowToast -> {
                        context?.showToast(event.message.asString(requireContext()))
                    }
                }
            }
        }

        val callback = object : AvatarResultAdapter.Callback {
            override fun onItemClick(position: Int, data: AvatarResultUiModel.AvatarItem) {
                gotoModelDetail(position, data)
            }
        }

        val adapter = AvatarResultAdapter(callback)

        val avatarResultListFlow = uiState.map { it.avatarResultList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            avatarResultListFlow.collectLatest { avatarResultList ->
                adapter.submitList(avatarResultList)
            }
        }

        avatarPreviewList.adapter = adapter

        bindClick(
            uiState = uiState,
            uiAction = uiAction
        )
    }

    private fun FragmentAvatarResultBinding.bindClick(
        uiState: StateFlow<AvatarResultState>,
        uiAction: (AvatarResultUiAction) -> Unit
    ) {
        icDownload.isVisible = false

        btnNext.text = getString(R.string.label_download)
        btnNext.setOnClickListener {
            if (ApplicationDependencies.getPersistentStore().isLogged) {
                val avatarStatus = uiState.value.avatarStatusWithFiles?.avatarStatus
                if (avatarStatus != null && avatarStatus.paid) {
                    // TODO: get folder name
                    context?.showToast("Getting folder name")
                    EditFolderNameDialog { typedName ->
                        if (typedName.isBlank()) {
                            return@EditFolderNameDialog "Name cannot be empty!"
                        }
                        if (typedName.length < 4) {
                            return@EditFolderNameDialog "Name too short"
                        }
                        // TODO: move 'save to gallery' to a foreground service
                        context?.showToast("Saved to $typedName")
                        ApplicationDependencies.getPersistentStore().apply {
                            setCurrentAvatarStatusId(null)
                            setUploadingPhotos(false)
                            setProcessingModel(false)
                        }
                        (activity as? MainActivity)?.restart()
                        null
                    }.show(childFragmentManager, "folder-name-dialog")
                } else {
                    // TODO: goto payment
                    findNavController().apply {
                        val args = bundleOf(
                            Constant.EXTRA_FROM to "login"
                        )
                        val navOpts = NavOptions.Builder()
                            .setEnterAnim(R.anim.fade_scale_in)
                            .setExitAnim(R.anim.fade_scale_out)
                            .setPopUpTo(R.id.login_fragment, inclusive = true, saveState = true)
                            .build()
                        navigate(R.id.subscription_plans, args, navOpts)
                    }
                }
            } else {
                findNavController().apply {
                    val args = bundleOf(
                        Constant.EXTRA_FROM to "avatar_result"
                    )
                    val navOpts = NavOptions.Builder()
                        .setEnterAnim(R.anim.fade_scale_in)
                        .setExitAnim(R.anim.fade_scale_out)
                        .setPopUpTo(R.id.avatar_result, inclusive = false, saveState = true)
                        .build()
                    navigate(R.id.login_fragment, args, navOpts)
                }
            }
        }

        icShare.isVisible = true
        icShare.setOnClickListener {  }

        btnClose.setOnClickListener { findNavController().navigateUp() }
    }

    private fun checkFolderName(name: String) {

    }

    private fun gotoModelDetail(position: Int, data: AvatarResultUiModel.AvatarItem) {
        val modelId = viewModel.getModelId()
        try {
            findNavController().apply {
                val navOpts = defaultNavOptsBuilder().build()
                val args = Bundle().apply {
                    putString(Constant.EXTRA_FROM, "result_preview")
                    putString(ModelDetailFragment.ARG_MODEL_ID, modelId)
                    putInt(ModelDetailFragment.ARG_JUMP_TO_POSITION, position)
                }
                navigate(R.id.model_detail, args, navOpts)
            }
        } catch (ignore: Exception) {}
    }
}

class AvatarResultAdapter(
    private val callback: Callback
) : ListAdapter<AvatarResultUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is ItemViewHolder) {
            model as AvatarResultUiModel.AvatarItem
            holder.bind(model, callback)
        }
    }

    class ItemViewHolder(
        private val binding: ItemSquareImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: AvatarResultUiModel.AvatarItem, callback: Callback) = with(binding) {
            Glide.with(view1)
                .load(data.avatar.remoteFile)
                .placeholder(R.color.grey_900)
                .into(view1)

            root.setOnClickListener { callback.onItemClick(adapterPosition, data) }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_square_image,
                    parent,
                    false
                )
                val binding = ItemSquareImageBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }

    }

    interface Callback {
        fun onItemClick(position: Int, data: AvatarResultUiModel.AvatarItem)
    }

    companion object {

        val DIFF_CALLBACK = object : ItemCallback<AvatarResultUiModel>() {
            override fun areItemsTheSame(
                oldItem: AvatarResultUiModel,
                newItem: AvatarResultUiModel,
            ): Boolean {
                return (oldItem is AvatarResultUiModel.AvatarItem && newItem is AvatarResultUiModel.AvatarItem &&
                        oldItem.avatar.id == newItem.avatar.id)
            }

            override fun areContentsTheSame(
                oldItem: AvatarResultUiModel,
                newItem: AvatarResultUiModel,
            ): Boolean {
                return true
            }
        }


    }
}