package com.aiavatar.app.feature.home.presentation.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.R
import com.aiavatar.app.databinding.CatalogItemBinding
import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.presentation.catalog.CatalogUiModel

class CatalogAdapter(
    private val callback: Callback
) : ListAdapter<CatalogUiModel, CatalogAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> ItemViewHolder.from(parent)
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val model = getItem(position)
        when (model) {
            is CatalogUiModel.Catalog -> {
                holder.bind(model.category, callback)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val model = getItem(position)) {
            is CatalogUiModel.Catalog -> VIEW_TYPE_ITEM
            else -> throw IllegalStateException("Can't decide a viewType for $model position: $position")
        }
    }

    class ItemViewHolder private constructor(
        private val binding: CatalogItemBinding
    ): ViewHolder(binding.root) {

        fun bind(data: Category, callback: Callback) = with(binding) {
            catalogTitle.text = data.title
            presetList.adapter = CatalogPreviewAdapter(
                binding.root.context,
            ) { position ->
                callback.onCardClicked(adapterPosition, position)
            }.apply {
                submitList(data.preset)
            }
            moreCatalog.setOnClickListener { callback.onMoreClicked(position = adapterPosition) }
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.catalog_item,
                    parent,
                    false
                )
                val binding = CatalogItemBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    interface Callback {
        fun onCardClicked(position: Int, cardPosition: Int)
        fun onMoreClicked(position: Int)
    }

    companion object {
        private val VIEW_TYPE_ITEM = 0

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CatalogUiModel>() {
            override fun areItemsTheSame(
                oldItem: CatalogUiModel,
                newItem: CatalogUiModel,
            ): Boolean {
                return (oldItem is CatalogUiModel.Catalog && newItem is CatalogUiModel.Catalog &&
                        oldItem.category.title == newItem.category.title)
            }

            override fun areContentsTheSame(
                oldItem: CatalogUiModel,
                newItem: CatalogUiModel,
            ): Boolean {
                return (oldItem is CatalogUiModel.Catalog && newItem is CatalogUiModel.Catalog &&
                        oldItem.category == newItem.category)
            }

        }
    }
}