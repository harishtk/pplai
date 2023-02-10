package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentWalthrough3Binding
import com.aiavatar.app.databinding.ItemCircleImageBinding
import com.aiavatar.app.getDisplaySize
import timber.log.Timber

class WalkThroughContent3 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_walthrough_3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWalthrough3Binding.bind(view)

        binding.bindState()
    }

    private fun FragmentWalthrough3Binding.bindState() {
        val colors = listOf(
            R.color.tag_first_color,
            R.color.tag_second_color,
            R.color.tag_third_color,
            R.color.tag_fourth_color,
            R.color.tag_fifth_color,
        )
        val row1 = listOf<Int>(
            R.drawable.walkthrough_3_r1c1,
            R.drawable.walkthrough_3_r1c2,
            R.drawable.walkthrough_3_r1c3,
            R.drawable.walkthrough_3_r1c4,
            R.drawable.walkthrough_3_r1c5,
            R.drawable.walkthrough_3_r1c6,
            R.drawable.walkthrough_3_r1c7,
            R.drawable.walkthrough_3_r1c8,
        )

        val row2 = listOf<Int>(
            R.drawable.walkthrough_3_r2c1,
            R.drawable.walkthrough_3_r2c2,
            R.drawable.walkthrough_3_r2c3,
            R.drawable.walkthrough_3_r2c4,
            R.drawable.walkthrough_3_r2c5,
            R.drawable.walkthrough_3_r2c6,
            R.drawable.walkthrough_3_r2c7,
            R.drawable.walkthrough_3_r2c8,
        )

        val row3 = listOf<Int>(
            R.drawable.walkthrough_3_r3c1,
            R.drawable.walkthrough_3_r3c2,
            R.drawable.walkthrough_3_r3c3,
            R.drawable.walkthrough_3_r3c4,
            R.drawable.walkthrough_3_r3c5,
            R.drawable.walkthrough_3_r3c6,
            R.drawable.walkthrough_3_r3c7,
            R.drawable.walkthrough_3_r3c8,
        )

        val adapter1 = CircleImageAdapter().apply {
            submitList(row1)
        }
        val adapter2 = CircleImageAdapter().apply {
            submitList(row2)
        }
        val adapter3 = CircleImageAdapter().apply {
            submitList(row3)
        }

        list1.adapter = adapter1
        list2.adapter = adapter2
        list3.adapter = adapter3

        val halfScreen = activity?.getDisplaySize()?.width?.div(2) ?: 0
        Timber.d("Half screen: $halfScreen")
        list1.apply {
            isLoopEnabled = true
            setCanTouch(false)
            startAutoScroll()
        }
        list2.apply {
            isLoopEnabled = true
            reverse = true
            setCanTouch(false)
            openAutoScroll(100, true)
        }

        list3.apply {
            isLoopEnabled = true
            setCanTouch(false)
            startAutoScroll()
        }
    }
}

class CircleImageAdapter : ListAdapter<Int, ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = getItem(position)
        if (holder is ItemViewHolder) {
            holder.bind(model)
        }
    }

    class ItemViewHolder(
        private val binding: ItemCircleImageBinding
    ) : ViewHolder(binding.root) {

        fun bind(@DrawableRes drawableRes: Int) = with(binding) {
            val context = binding.root.context
            view1.setImageResource(drawableRes)
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_circle_image,
                    parent,
                    false
                )
                val binding = ItemCircleImageBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }

    }

    companion object {
        val DIFF_CALLBACK = object : ItemCallback<Int>() {
            override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }

        }
    }
}