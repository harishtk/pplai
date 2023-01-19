package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentWalthrough2Binding
import com.aiavatar.app.databinding.ItemCircleImageBinding
import com.aiavatar.app.databinding.ItemSquareImageBinding
import com.aiavatar.app.getRandomHexCode

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

        val resList = listOf<Int>(
            R.drawable.wt_small_grid_1,
            R.drawable.wt_small_grid_2,
            R.drawable.wt_small_grid_3,
            R.drawable.wt_small_grid_4,
            R.drawable.wt_small_grid_5,
            R.drawable.wt_small_grid_6,
            R.drawable.wt_small_grid_7,
            R.drawable.wt_small_grid_8,
            R.drawable.wt_small_grid_9,
            R.drawable.wt_small_grid_10,
            R.drawable.wt_small_grid_11,
            R.drawable.wt_small_grid_12,
        )
        adapter.submitList(resList)
    }
}

class SquareImageAdapter : ListAdapter<Int, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is ItemViewHolder) {
            holder.bind(model)
        }
    }

    class ItemViewHolder(
        private val binding: ItemSquareImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(@DrawableRes resImage: Int) = with(binding) {
            view1.setImageResource(resImage)
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

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Int>() {
            override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }

        }
    }
}