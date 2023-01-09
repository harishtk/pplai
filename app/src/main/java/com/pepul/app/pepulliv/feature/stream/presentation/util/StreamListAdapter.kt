package com.pepul.app.pepulliv.feature.stream.presentation.util

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.pepul.app.pepulliv.Constant
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STARTED
import com.pepul.app.pepulliv.MainActivity.Companion.getThumbnail
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.commons.util.DateUtil
import com.pepul.app.pepulliv.commons.util.recyclerview.Recyclable
import com.pepul.app.pepulliv.databinding.StreamItemBinding
import com.pepul.app.pepulliv.feature.stream.data.source.remote.model.StreamDto
import com.pepul.app.pepulliv.feature.stream.presentation.publish.DefaultPublishFragment
import com.pepul.app.pepulliv.feature.stream.presentation.streamlist.StreamUiModel

class StreamListAdapter(
    private val context: Context,
    private val callback: Callback
) : ListAdapter<StreamUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> ItemViewHolder.from(parent)
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)
        when (holder) {
            is ItemViewHolder -> {
                model as StreamUiModel.Item
                holder.bind(model, callback)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is StreamUiModel.Item -> VIEW_TYPE_ITEM
            else -> throw IllegalStateException("Unknown item type")
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? Recyclable)?.onViewRecycled()
    }

    class ItemViewHolder private constructor(
        private val binding: StreamItemBinding
    ) : RecyclerView.ViewHolder(binding.root), Recyclable {

        fun bind(model: StreamUiModel.Item, callback: Callback) = with(binding) {
            // TODO: bind to view
            val startTime = DateUtil.parseUtcStringToTimeAgo(model.streamItem.createdAt)
            streamTimeText.text = startTime /*"${model.streamItem.streamName} · $startTime"*/

            if (model.streamItem.state == STREAM_STATE_STARTED) {
                liveIndicator.isVisible = true
            } else {
                liveIndicator.isVisible = false
            }

            Glide.with(root.context)
                 .load(getThumbnail(model.streamItem.streamId ?: ""))
                 .placeholder(R.color.black)
                 .diskCacheStrategy(DiskCacheStrategy.NONE)
                 .skipMemoryCache(true)
                 .into(thumbnail)

            root.setOnClickListener { callback.onStreamItemClick(model.streamItem) }
            root.setOnLongClickListener {
                callback.onStreamDeleteClick(model.streamItem)
                true
            }
        }

        override fun onViewRecycled() = with(binding) {
            thumbnail.setImageDrawable(null)
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.stream_item, parent, false)
                val binding = StreamItemBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }

    }

    interface Callback {
        fun onStreamItemClick(streamItem: StreamDto)
        fun onStreamDeleteClick(streamItem: StreamDto)
    }

    companion object {
        const val VIEW_TYPE_ITEM = 0

        const val PAYLOAD_TIMESTAMP = "timestamp"
        const val PAYLOAD_THUMBNAIL = "thumbnail"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<StreamUiModel>() {
            override fun areItemsTheSame(oldItem: StreamUiModel, newItem: StreamUiModel): Boolean {
                return (oldItem is StreamUiModel.Item && newItem is StreamUiModel.Item &&
                        oldItem.streamItem.id == newItem.streamItem.id)
            }

            override fun areContentsTheSame(
                oldItem: StreamUiModel,
                newItem: StreamUiModel,
            ): Boolean {
                return (oldItem is StreamUiModel.Item && newItem is StreamUiModel.Item &&
                        oldItem.streamItem == newItem.streamItem)
            }

            override fun getChangePayload(oldItem: StreamUiModel, newItem: StreamUiModel): Any? {
                // TODO: parse payload and send update
                return super.getChangePayload(oldItem, newItem)
            }
        }
    }
}