package com.aiavatar.app.feature.home.presentation.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aiavatar.app.R
import com.aiavatar.app.core.URLProvider
import com.aiavatar.app.databinding.ItemBigAvatarBinding
import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.presentation.catalog.AvatarUiModel
import com.bumptech.glide.Glide
import timber.log.Timber

/**
 * Note: [loopEnable] currently uni-directional i.e. like infinite scroll.
 */
class AvatarsAdapter(
    private val layoutManager: LayoutManager,
    private val callback: Callback,
    private val loopEnable: Boolean = false,
) : ListAdapter<AvatarUiModel, ViewHolder>(DIFF_CALLBACK) {

    /**
     * No idea what this thing does.!
     */
    private fun getHeightRatioForPosition(position: Int, totalRows: Int): String {
        val isLastRow: Boolean = checkIfPositionIsLastRow(position)
        val ratio = if (position % 2 != 0) {
            val relativePosition = position % 4
            if (relativePosition == 1) {
                String.format("%d:%d", 1, 1)
            } else if (relativePosition == 3 ) {
                String.format("%d:%d", 5, 7)
            } else {
                String.format("%d:%d", 3, 4)
            }
        } else {
            String.format("%d:%d", 3, 4)
        }
        return ratio
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val normalizedPosition = generatePosition(position)
        val model = getItem(normalizedPosition)
        if (holder is ItemViewHolder) {
            model as AvatarUiModel.AvatarItem
            val heightRatio = getHeightRatioForPosition(normalizedPosition, getTotalRows())
            holder.bind(model.category, heightRatio, callback)
        }
    }

    private fun getTotalRows(): Int {
        val gridLayoutManager = (layoutManager as? StaggeredGridLayoutManager)
        return if (gridLayoutManager != null) {
            itemCount / gridLayoutManager.spanCount
        } else {
            0
        }
    }

    private fun checkIfPositionIsLastRow(position: Int): Boolean {
        val gridLayoutManager = (layoutManager as? StaggeredGridLayoutManager)
        return if (gridLayoutManager != null) {
            val totalRows = getTotalRows()
            (position > totalRows && position < itemCount - 1)
        } else {
            false
        }
    }

    /**
     * Returns the corresponding position according to the current scroll mode
     */
    private fun generatePosition(position: Int): Int {
        return if (getLoopEnable()) {
            getActualPosition(position)
        } else {
            position
        }
    }

    /**
     * Returns the actual position of the item
     *
     * @param position The position after starting to scroll will grow indefinitely
     * @return Item actual location
     */
    private fun getActualPosition(position: Int): Int {
        val itemCount: Int = super.getItemCount()
        return if (position >= itemCount) position % itemCount else position
    }

    private fun getLoopEnable() = loopEnable

    override fun getItemCount(): Int {
        //If it is an infinite scroll mode, set an unlimited number of items
        val itemCount = super.getItemCount()
        return if (getLoopEnable() && itemCount > 0) Int.MAX_VALUE else itemCount
    }

    class ItemViewHolder private constructor(
        private val binding: ItemBigAvatarBinding,
    ) : ViewHolder(binding.root) {

        private val cardConstraintSet = ConstraintSet()

        fun bind(category: Category, heightRatio: String, callback: Callback) = with(binding) {
            val url = URLProvider.avatarUrl(category.imageName)
            Glide.with(image)
                .load(url)
                .placeholder(R.drawable.loading_animation)
                .into(image)
            textCategory.text = category.categoryName

            cardConstraintSet.clone(cardContent)
            cardConstraintSet.setDimensionRatio(image.id, heightRatio)
            cardConstraintSet.applyTo(cardContent)

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