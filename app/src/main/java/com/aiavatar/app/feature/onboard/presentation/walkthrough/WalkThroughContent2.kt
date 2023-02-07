package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.R
import com.aiavatar.app.databinding.*

class WalkThroughContent2 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_walthrough_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWalthrough2Binding.bind(view)

        binding.bindState()
    }

    private fun FragmentWalthrough2Binding.bindState() {
        val adapter = SquareImageAdapter()
        list1.adapter = adapter

        (list1.layoutManager as? GridLayoutManager)?.let { gridLayoutManager ->
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return adapter.getSpanSize(position)
                }
            }
        }

        val header = SquareImageUiModel.Header(
            getString(R.string.walkthrough2_title_1),
            spanCount = 4
        )

        val resList = listOf<SquareImageUiModel>(
            header,
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_1), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_2), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_3), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_4), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_5), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_6), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_7), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_8), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_9), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_10), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_11), spanCount = 1),
            SquareImageUiModel.Item(SquareImageItem(R.drawable.wt_small_grid_12), spanCount = 1),
        )
        adapter.submitList(resList)
    }
}

data class SquareImageItem(
    @DrawableRes
    val drawableRes: Int
)

sealed class SquareImageUiModel {
    abstract val spanCount: Int

    data class Item(val data: SquareImageItem, override val spanCount: Int) : SquareImageUiModel()
    data class Header(val title: String, override val spanCount: Int) : SquareImageUiModel()
    data class Description(val description: String, override val spanCount: Int) : SquareImageUiModel()
}

class SquareImageAdapter : ListAdapter<SquareImageUiModel, ViewHolder>(DIFF_CALLBACK) {

    fun getSpanSize(position: Int): Int {
        return getItem(position).spanCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> ItemViewHolder.from(parent)
            VIEW_TYPE_HEADER -> TitleViewHolder.from(parent)
            VIEW_TYPE_DESCRIPTION -> DescriptionViewHolder.from(parent)
            else -> throw IllegalStateException("Unknown view type = $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = getItem(position)
        when (holder) {
            is ItemViewHolder -> {
                model as SquareImageUiModel.Item
                holder.bind(model)
            }
            is TitleViewHolder -> {
                model as SquareImageUiModel.Header
                holder.bind(model.title)
            }
            is DescriptionViewHolder -> {
                model as SquareImageUiModel.Description
                holder.bind(model.description)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val model = getItem(position)) {
            is SquareImageUiModel.Item -> VIEW_TYPE_ITEM
            is SquareImageUiModel.Header -> VIEW_TYPE_HEADER
            is SquareImageUiModel.Description -> VIEW_TYPE_DESCRIPTION
            else -> throw IllegalStateException("Can't decide a view type for $model")
        }
    }

    class DescriptionViewHolder private constructor(
        private val binding: ItemDescriptionBinding
    ) : ViewHolder(binding.root) {
        fun bind(title: String) = with(binding) {
            textView1.text = title
        }

        companion object {
            fun from(parent: ViewGroup): DescriptionViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_description,
                    parent,
                    false
                )
                val binding = ItemDescriptionBinding.bind(itemView)
                return DescriptionViewHolder(binding)
            }
        }
    }

    class TitleViewHolder private constructor(
        private val binding: ItemTitleBinding
    ) : ViewHolder(binding.root) {
        fun bind(title: String) = with(binding) {
            textView1.text = title
        }

        companion object {
            fun from(parent: ViewGroup): TitleViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_title,
                    parent,
                    false
                )
                val binding = ItemTitleBinding.bind(itemView)
                return TitleViewHolder(binding)
            }
        }
    }

    class ItemViewHolder private constructor(
        private val binding: ItemSquareImageBinding
    ) : ViewHolder(binding.root) {

        fun bind(data: SquareImageUiModel.Item) = with(binding) {
            view1.setImageResource(data.data.drawableRes)
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_square_image,
                    parent,
                    false
                )
                val binding = ItemSquareImageBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }

    }

    companion object {
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_HEADER = 1
        const val VIEW_TYPE_DESCRIPTION = 2

        val DIFF_CALLBACK_OLD = object : DiffUtil.ItemCallback<Int>() {
            override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }
        }

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SquareImageUiModel>() {
            override fun areContentsTheSame(
                oldItem: SquareImageUiModel,
                newItem: SquareImageUiModel,
            ): Boolean {
                return (oldItem is SquareImageUiModel.Item && newItem is SquareImageUiModel.Item &&
                        oldItem.data.drawableRes == newItem.data.drawableRes)
            }

            override fun areItemsTheSame(
                oldItem: SquareImageUiModel,
                newItem: SquareImageUiModel,
            ): Boolean {
                return false
            }

        }
    }
}