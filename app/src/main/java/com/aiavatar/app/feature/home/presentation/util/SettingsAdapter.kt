package com.aiavatar.app.feature.home.presentation.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.R
import com.aiavatar.app.databinding.ItemSimpleSettingsBinding

class SettingsAdapter(
    private val callback: Callback
) : ListAdapter<SettingsItem, ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SIMPLE_ITEM -> SimpleItemViewHolder.from(parent)
            else -> throw IllegalStateException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is SimpleItemViewHolder) {
            holder.bind(model, callback)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).settingsListType) {
            SettingsListType.SIMPLE -> VIEW_TYPE_SIMPLE_ITEM
        }
    }

    class SimpleItemViewHolder private constructor(
        private val binding: ItemSimpleSettingsBinding
    ): ViewHolder(binding.root) {

        fun bind(data: SettingsItem, callback: Callback) = with(binding) {
            settingsTitle.text = data.title
            if (data.description?.isNotBlank() == true) {
                settingsDescription.isVisible = true
                settingsDescription.text = data.description
            } else {
                settingsDescription.isVisible = false
            }

            if (data.icon != null) {
                settingsIcon.setImageResource(data.icon)
            } else {
                settingsIcon.setImageResource(R.drawable.ic_settings_filled)
            }

            binding.root.setOnClickListener { callback.onItemClick(adapterPosition) }
        }

        companion object {
            fun from(parent: ViewGroup): SimpleItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_simple_settings,
                    parent,
                    false
                )
                val binding = ItemSimpleSettingsBinding.bind(itemView)
                return SimpleItemViewHolder(binding)
            }
        }
    }

    interface Callback {
        fun onItemClick(position: Int)
    }

    companion object {
        private const val VIEW_TYPE_SIMPLE_ITEM = 0

        val DIFF_CALLBACK = object : ItemCallback<SettingsItem>() {
            override fun areItemsTheSame(oldItem: SettingsItem, newItem: SettingsItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SettingsItem, newItem: SettingsItem): Boolean {
                return oldItem == newItem
            }

        }
    }
}