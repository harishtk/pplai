package com.aiavatar.app.feature.home.presentation.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.aiavatar.app.Constant
import com.aiavatar.app.R
import com.aiavatar.app.databinding.ItemBigAvatarBinding
import com.aiavatar.app.feature.home.domain.model.Avatar
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
            holder.bind(model.avatar, callback)
        }
    }

    class ItemViewHolder private constructor(
        private val binding: ItemBigAvatarBinding
    ) : ViewHolder(binding.root) {

        fun bind(avatar: Avatar, callback: Callback) = with(binding) {
            val url = "${Constant.AVATAR_IMAGE_PREFIX}${avatar.imageUrl}"
            Glide.with(image)
                .load(url)
                .placeholder(R.color.grey_divider)
                .into(image)
            textCategory.text = avatar.categoryName

            binding.root.setOnClickListener { callback.onItemClick(adapterPosition) }
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
        fun onItemClick(position: Int)
    }

    companion object {
        private val VIEW_TYPE_ITEM = 0

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AvatarUiModel>() {
            override fun areItemsTheSame(
                oldItem: AvatarUiModel,
                newItem: AvatarUiModel,
            ): Boolean {
                return (oldItem is AvatarUiModel.AvatarItem && newItem is AvatarUiModel.AvatarItem &&
                        oldItem.avatar.id == newItem.avatar.id)
            }

            override fun areContentsTheSame(
                oldItem: AvatarUiModel,
                newItem: AvatarUiModel,
            ): Boolean {
                return (oldItem is AvatarUiModel.AvatarItem && newItem is AvatarUiModel.AvatarItem &&
                        oldItem.avatar == newItem.avatar)
            }

        }
    }
}