package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentWalthrough3Binding
import com.aiavatar.app.databinding.ItemCircleImageBinding

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

        val adapter = CircleImageAdapter()
        list1.adapter = adapter
        list2.adapter = adapter
        list3.adapter = adapter

        adapter.submitList(colors)

        /*val layoutManager = object : LinearLayoutManager(context, RecyclerView.HORIZONTAL, false) {

            override fun smoothScrollToPosition(
                recyclerView: RecyclerView?,
                state: RecyclerView.State?,
                position: Int
            ) {
                val smoothScroller = object : LinearSmoothScroller(context) {
                     override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                         *//*return (2000F / displayMetrics!!.densityDpi).also {
                             Timber.d("Speed: $it")
                         }*//*
                         return super.calculateSpeedPerPixel(displayMetrics)
                     }
                }
                smoothScroller.targetPosition = position
                startSmoothScroll(smoothScroller)
            }
        }
        list1.layoutManager = layoutManager*/

       /* val speed = 1000L
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            var count = 0

            override fun run() {
                if (count == adapter.itemCount) {
                    count = 0
                }
                if (count < adapter.itemCount) {
                    val jumpTo = count++
                    Timber.d("Jump To: $jumpTo")
                    list1.smoothScrollToPosition(jumpTo)
                    handler.postDelayed(this, speed)
                }
            }

        }
        handler.postDelayed(runnable, 0)*/
        list1.apply {
            isLoopEnabled = true
            setCanTouch(false)
            startAutoScroll()
        }
        list2.apply {
            isLoopEnabled = true
            reverse = true
            setCanTouch(false)
            openAutoScroll(30, true)
            /*list2.postDelayed({
                list2.startAutoScroll()
            }, 1000)*/
        }

        list3.apply {
            isLoopEnabled = true
            setCanTouch(false)
            startAutoScroll()
            /*list3.postDelayed({
                list3.startAutoScroll()
            }, 1000)*/
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

        fun bind(@ColorRes color: Int) = with(binding) {
            val context = binding.root.context
            view1.setImageDrawable(ColorDrawable(context.getColor(color)))
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