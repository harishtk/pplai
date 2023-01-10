package com.pepulai.app.feature.home.presentation.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.pepulai.app.R
import com.pepulai.app.databinding.CatalogItemBinding
import com.pepulai.app.feature.home.domain.model.Category
import com.pepulai.app.feature.home.presentation.catalog.CatalogUiModel

class CatalogAdapter : ListAdapter<CatalogUiModel, CatalogAdapter.ItemViewHolder>(DIFF_CALLBACK) {

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
                holder.bind(model.category)
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

        fun bind(data: Category) = with(binding) {
            catalogTitle.text = data.title
            presetList.adapter = CatalogPreviewAdapter(binding.root.context).apply {
                submitList(data.preset)
            }
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