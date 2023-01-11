package com.pepulai.app.feature.onboard.presentation.walkthrough

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pepulai.app.R
import com.pepulai.app.databinding.FragmentWalthrough2Binding
import com.pepulai.app.databinding.ItemCircleImageBinding
import com.pepulai.app.getRandomHexCode

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
        val colors = (0..15).map { Color.parseColor(getRandomHexCode()) }.toList()

        val adapter = SquareImageAdapter()
        list1.adapter = adapter

        adapter.submitList(colors)
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
        private val binding: ItemCircleImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(color: Int) = with(binding) {
            view1.setImageDrawable(ColorDrawable(color))
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_square_image,
                    parent,
                    false
                )
                val binding = ItemCircleImageBinding.bind(itemView)
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