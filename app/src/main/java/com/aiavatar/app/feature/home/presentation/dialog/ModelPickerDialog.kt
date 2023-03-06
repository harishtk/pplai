package com.aiavatar.app.feature.home.presentation.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.imageloader.GlideImageLoader.Companion.newGlideBuilder
import com.aiavatar.app.core.URLProvider
import com.aiavatar.app.core.fragment.BaseBottomSheetDialogFragment
import com.aiavatar.app.databinding.DialogModelPickerBinding
import com.aiavatar.app.databinding.ItemAvatarStatusBinding
import com.aiavatar.app.databinding.ItemModelListAltBinding
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.presentation.profile.ProfileListUiModel
import com.aiavatar.app.getHtmlSpannedString
import com.aiavatar.app.nullAsEmpty
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 * @author Hariskumar Kubendran
 * @date 06/03/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
@AndroidEntryPoint
class ModelPickerDialog(
    private val onModelClick: (modelId: String) -> Boolean,
    private val onSkip: () -> Unit
) : BaseBottomSheetDialogFragment(R.color.bottom_sheet_background) {

    private val viewModel: ModelPickerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_model_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogModelPickerBinding.bind(view)

        binding.bindState(
            uiState = viewModel.uiState
        )
    }

    private fun DialogModelPickerBinding.bindState(uiState: StateFlow<ModelPickerState>) {
        // TODO: bind model list
        // TODO: click action

        description.text =
            resources.getHtmlSpannedString(R.string.model_picker_description)

        val callback = object : ModelListAdapter.Callback {
            override fun onItemClick(position: Int, data: ProfileListUiModel.Item) {
                data.modelListWithModel.model?.id?.let { modelId ->
                    if (onModelClick(modelId)) {
                        dismiss()
                    }
                }
            }
        }
        val adapter = ModelListAdapter(
            glide = initGlide(),
            callback = callback
        )

        modelListView.adapter = adapter

        val modelListUiModelListFlow = uiState.map { it.modelsUiModelList }
            .distinctUntilChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            modelListUiModelListFlow.collectLatest { data ->
                Timber.d("Model List: size ${data.size}")
                adapter.submitList(data)
            }
        }

        bindClick()
    }

    private fun DialogModelPickerBinding.bindClick() {
        btnClose.setOnClickListener {
            dismiss()
        }
        btnSave.setOnClickListener {
            dismiss()
            onSkip.invoke()
        }
    }

    private fun initGlide(): RequestManager {
        return Glide.with(this)
    }

    class ModelListAdapter(
        private val glide: RequestManager,
        private val callback: Callback,
    ) : ListAdapter<ProfileListUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

        private lateinit var parent: RecyclerView

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_ITEM -> ItemViewHolder.from(parent)
                VIEW_TYPE_THINKING -> AvatarStatusViewHolder.from(parent)
                else -> {
                    throw IllegalStateException("Unknown viewType $viewType")
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val model = getItem(position)
            val cardMaxWidth = ApplicationDependencies.getDisplaySize().width.div(2.5F).toInt()
            when (model) {
                is ProfileListUiModel.Item -> {
                    when (holder) {
                        is ModelListAdapter.ItemViewHolder -> {
                            holder.bind(data = model, glide, callback, cardMaxWidth)
                        }
                        is AvatarStatusViewHolder -> {
                            holder.bind(data = model, callback, cardMaxWidth)
                        }
                    }
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (val model = getItem(position)) {
                is ProfileListUiModel.Item -> {
                    if (model.modelListWithModel.modelListItem.statusId != "0") {
                        VIEW_TYPE_THINKING
                    } else {
                        VIEW_TYPE_ITEM
                    }
                }
                else -> {
                    throw IllegalStateException("Can't decide a view type for $position")
                }
            }
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            this.parent = recyclerView
        }

        class AvatarStatusViewHolder private constructor(
            private val binding: ItemAvatarStatusBinding,
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(data: ProfileListUiModel.Item, callback: Callback, width: Int) = with(binding) {
                (root.layoutParams as ViewGroup.LayoutParams).let { lp ->
                    lp.width = width.toInt()
                    root.layoutParams = lp
                }
                title.text = "Generating.."
                description.isVisible = false

                itemView.setOnClickListener { callback.onItemClick(adapterPosition, data) }
            }

            companion object {
                fun from(parent: ViewGroup): AvatarStatusViewHolder {
                    val itemView = LayoutInflater.from(parent.context).inflate(
                        R.layout.item_avatar_status,
                        parent,
                        false
                    )
                    val binding = ItemAvatarStatusBinding.bind(itemView)
                    return AvatarStatusViewHolder(binding)
                }
            }
        }

        class ItemViewHolder private constructor(
            private val binding: ItemModelListAltBinding,
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(
                data: ProfileListUiModel.Item,
                glide: RequestManager,
                callback: Callback,
                width: Int
            ) = with(binding) {

                (root.layoutParams as ViewGroup.LayoutParams).let { lp ->
                    lp.width = width.toInt()
                    root.layoutParams = lp
                }
                title.text = data.modelListWithModel.model?.name
                description.text = "${data.modelListWithModel.model?.totalCount} creations"

                val imageUrl: String = data.modelListWithModel.model.let { modelData ->
                    if (modelData?.thumbnail?.isNotBlank() == true) {
                        URLProvider.avatarThumbUrl(modelData.thumbnail)!!
                    } else {
                        modelData?.latestImage.nullAsEmpty()
                    }
                }

                imageView.apply {
                    newGlideBuilder(glide)
                        .originalImage(imageUrl)
                        .placeholder(R.drawable.loading_animation)
                        .error(R.color.white)
                        .start()
                }

                // toggleSelection(selected)

                imageView.setOnClickListener { callback.onItemClick(adapterPosition, data) }
            }

            companion object {
                fun from(parent: ViewGroup): ItemViewHolder {
                    val itemView = LayoutInflater.from(parent.context).inflate(
                        R.layout.item_model_list_alt,
                        parent,
                        false
                    )
                    val binding = ItemModelListAltBinding.bind(itemView)
                    return ItemViewHolder(binding)
                }
            }
        }

        interface Callback {
            fun onItemClick(position: Int, data: ProfileListUiModel.Item)
        }

        companion object {
            private const val VIEW_TYPE_ITEM = 0
            private const val VIEW_TYPE_THINKING = 1

            private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ProfileListUiModel>() {
                override fun areItemsTheSame(
                    oldItem: ProfileListUiModel,
                    newItem: ProfileListUiModel,
                ): Boolean {
                    return (oldItem is ProfileListUiModel.Item && newItem is ProfileListUiModel.Item &&
                            oldItem.modelListWithModel.model?.id == newItem.modelListWithModel.model?.id)
                }

                override fun areContentsTheSame(
                    oldItem: ProfileListUiModel,
                    newItem: ProfileListUiModel,
                ): Boolean {
                    return false
                }
            }
        }

    }
}

