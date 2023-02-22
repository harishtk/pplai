package com.aiavatar.app.feature.home.presentation.util

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.imageloader.GlideImageLoader.Companion.disposeGlideLoad
import com.aiavatar.app.commons.util.recyclerview.Recyclable
import com.aiavatar.app.databinding.LargePresetPreviewBinding
import com.aiavatar.app.feature.home.domain.model.ModelAvatar
import com.aiavatar.app.feature.home.presentation.catalog.SelectableAvatarUiModel
import com.bumptech.glide.Glide
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class CatalogPagerAdapter(
    private val context: Context,
    private val onCardClick: (position: Int) -> Unit = { }
): ListAdapter<SelectableAvatarUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is ItemViewHolder) {
            model as SelectableAvatarUiModel.Item
            holder.bind(model.modelAvatar, model.selected, onCardClick)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            if (isValidPayload(payloads)) {
                val bundle = (payloads.firstOrNull() as? Bundle) ?: kotlin.run {
                    super.onBindViewHolder(holder, position, payloads); return
                }
                if (bundle.containsKey(SELECTION_TOGGLE_PAYLOAD)) {
                    (holder as? ItemViewHolder)?.toggleSelection(bundle.getBoolean(
                        SELECTION_TOGGLE_PAYLOAD, false))
                }
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun isValidPayload(payloads: MutableList<Any>?): Boolean {
        return (payloads?.firstOrNull() as? Bundle)?.keySet()?.any {
            it == SELECTION_TOGGLE_PAYLOAD
        } ?: false
    }

    class ItemViewHolder private constructor(
        private val binding: LargePresetPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root), Recyclable {

        fun bind(listAvatar: ModelAvatar, selected: Boolean, onCardClick: (position: Int) -> Unit) = with(binding) {
            val radius = binding.root.resources.getDimensionPixelSize(R.dimen.default_corner_size)
            title.text = listAvatar.remoteFile

            Glide.with(previewImage)
                .load(listAvatar.remoteFile)
                .placeholder(R.color.transparent_black)
                .error(R.color.white)
                // .transform(RoundedCornersTransformation(radius, 0, RoundedCornersTransformation.CornerType.ALL))
                .into(previewImage)

            // toggleSelection(selected)

            previewImage.setOnClickListener { onCardClick(adapterPosition) }
        }

        fun toggleSelection(selected: Boolean) = with(binding) {
            previewImage.alpha = if (selected) {
                1.0F
            } else {
                0.0F
            }
        }

        override fun onViewRecycled() {
            binding.previewImage.disposeGlideLoad()
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.large_preset_preview,
                    parent,
                    false
                )
                val binding = LargePresetPreviewBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    companion object {
        private const val SELECTION_TOGGLE_PAYLOAD = "selection_toggle"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SelectableAvatarUiModel>() {
            override fun areItemsTheSame(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel,
            ): Boolean {
                return (oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item &&
                        oldItem.modelAvatar._id == newItem.modelAvatar._id)
            }

            override fun areContentsTheSame(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel,
            ): Boolean {
                return (oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item &&
                        oldItem.modelAvatar == newItem.modelAvatar && oldItem.selected == newItem.selected)
            }

            override fun getChangePayload(
                oldItem: SelectableAvatarUiModel,
                newItem: SelectableAvatarUiModel
            ): Any? {
                /*val updatePayload = bundleOf()
                when {
                    oldItem is SelectableAvatarUiModel.Item && newItem is SelectableAvatarUiModel.Item -> {
                        if (oldItem.selected != newItem.selected) {
                            updatePayload.putBoolean(SELECTION_TOGGLE_PAYLOAD, newItem.selected)
                        }
                    }
                }
                return updatePayload*/
                return null
            }
        }
    }
}