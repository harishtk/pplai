package com.pepulai.app.feature.home.presentation.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.contentValuesOf
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.pepulai.app.R
import com.pepulai.app.commons.util.recyclerview.Recyclable
import com.pepulai.app.databinding.LargePresetPreviewBinding
import com.pepulai.app.feature.home.domain.model.CategoryPreset
import timber.log.Timber

class CatalogPagerAdapter(
    private val context: Context,
    private val onCardClick: (position: Int) -> Unit = { }
): ListAdapter<CategoryPreset, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(getItem(position), onCardClick)
        }
    }

    class ItemViewHolder private constructor(
        private val binding: LargePresetPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root), Recyclable {

        fun bind(preset: CategoryPreset, onCardClick: (position: Int) -> Unit) = with(binding) {
            title.text = preset.imageUrl
            Glide.with(previewImage)
                .load(preset.imageUrl)
                .placeholder(R.color.transparent_black)
                .error(R.color.white)
                .into(previewImage)
            previewImage.setOnClickListener { onCardClick(adapterPosition) }
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
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CategoryPreset>() {
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