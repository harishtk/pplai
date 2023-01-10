package com.pepulai.app.feature.home.presentation.util

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.pepulai.app.R
import com.pepulai.app.commons.util.recyclerview.Recyclable
import com.pepulai.app.databinding.PresetPreviewItemBinding
import com.pepulai.app.feature.home.domain.model.CategoryPreset

class CatalogPreviewAdapter(
    context: Context
): ListAdapter<CategoryPreset, ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(getItem(position))
        }
    }

    class ItemViewHolder private constructor(
        private val binding: PresetPreviewItemBinding
    ) : ViewHolder(binding.root), Recyclable {

        fun bind(preset: CategoryPreset) = with(binding) {
            Glide.with(previewImage)
                .load(preset.imageUrl)
                .placeholder(R.color.transparent_black)
                .into(previewImage)
        }

        override fun onViewRecycled() {
            binding.previewImage.let { imageView ->
                Glide.with(imageView).clear(null)
                imageView.setImageDrawable(null)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.preset_preview_item,
                    parent,
                    false
                )
                val binding = PresetPreviewItemBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : ItemCallback<CategoryPreset>() {
            override fun areItemsTheSame(
                oldItem: CategoryPreset,
                newItem: CategoryPreset,
            ): Boolean {
                return oldItem.prompt == newItem.prompt
            }

            override fun areContentsTheSame(
                oldItem: CategoryPreset,
                newItem: CategoryPreset,
            ): Boolean {
                return oldItem == newItem
            }

        }
    }
}