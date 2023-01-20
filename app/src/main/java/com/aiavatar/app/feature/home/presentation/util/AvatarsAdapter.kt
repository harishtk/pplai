package com.aiavatar.app.feature.home.presentation.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.aiavatar.app.Constant
import com.aiavatar.app.R
import com.aiavatar.app.core.URLProvider
import com.aiavatar.app.databinding.ItemBigAvatarBinding
import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.presentation.catalog.AvatarUiModel

class AvatarsAdapter(
    private val callback: Callback
) : ListAdapter<AvatarUiModel, ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is ItemViewHolder) {
            model as AvatarUiModel.AvatarItem
            holder.bind(model.category, callback)
        }
    }

    class ItemViewHolder private constructor(
        private val binding: ItemBigAvatarBinding
    ) : ViewHolder(binding.root) {

        fun bind(category: Category, callback: Callback) = with(binding) {
            val url = URLProvider.avatarUrl(category.imageName)
            Glide.with(image)
                .load(url)
                .placeholder(R.color.grey_divider)
                .into(image)
            textCategory.text = category.categoryName

            binding.root.setOnClickListener { callback.onItemClick(adapterPosition, category) }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_big_avatar,
                    parent,
                    false
                )
                val binding = ItemBigAvatarBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    interface Callback {
        fun onItemClick(position: Int, category: Category)
    }

    companion object {
        private val VIEW_TYPE_ITEM = 0

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AvatarUiModel>() {
            override fun areItemsTheSame(
                oldItem: AvatarUiModel,
                newItem: AvatarUiModel,
            ): Boolean {
                return (oldItem is AvatarUiModel.AvatarItem && newItem is AvatarUiModel.AvatarItem &&
                        oldItem.category.id == newItem.category.id)
            }

            override fun areContentsTheSame(
                oldItem: AvatarUiModel,
                newItem: AvatarUiModel,
            ): Boolean {
                return (oldItem is AvatarUiModel.AvatarItem && newItem is AvatarUiModel.AvatarItem &&
                        oldItem.category == newItem.category)
            }

        }
    }
}