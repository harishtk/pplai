package com.pepul.app.pepulliv.feature.stream.presentation.util

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.commons.util.recyclerview.Recyclable
import com.pepul.app.pepulliv.databinding.CommentItemBinding
import com.pepul.app.pepulliv.feature.stream.domain.model.CommentItem
import com.pepul.app.pepulliv.feature.stream.presentation.publish.haishinkit.CommentsUiModel

class LiveCommentsAdapter(
    val autoHideMillis: Long = DEFAULT_AUTO_HIDE_MILLIS
) : ListAdapter<CommentsUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> CommentViewHolder.from(parent)
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)
        when (holder) {
            is CommentViewHolder -> {
                model as CommentsUiModel.Comment
                holder.bind(model.commentItem, model.expired, autoHideMillis)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? Recyclable)?.onViewRecycled()
        super.onViewRecycled(holder)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CommentsUiModel.Comment -> VIEW_TYPE_ITEM
            else -> throw IllegalStateException("Cannot determine a item type for position $position")
        }
    }

    class CommentViewHolder(
        val binding: CommentItemBinding
    ) : RecyclerView.ViewHolder(binding.root), Recyclable {

        fun bind(commentItem: CommentItem, expired: Boolean, autoHideMillis: Long) = with(binding) {
            name.text = "@${commentItem.userId}"
            message.text = commentItem.content

            if (expired) {
                AnimationUtils.loadAnimation(binding.root.context, R.anim.fade_out_anim).apply {
                    duration = 1000
                    interpolator = LinearInterpolator()
                    root.startAnimation(this)
                }
            } else {
                root.alpha = 1.0f
            }
        }

        override fun onViewRecycled() = with(binding) {
            try { root.clearAnimation() }
            catch (ignore: Exception) {}
        }

        companion object {
            fun from(parent: ViewGroup): CommentViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.comment_item,
                    parent,
                    false
                )
                val binding = CommentItemBinding.bind(itemView)
                return CommentViewHolder(binding)
            }
        }
    }

    companion object {
        const val VIEW_TYPE_ITEM = 0

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CommentsUiModel>() {
            override fun areItemsTheSame(
                oldItem: CommentsUiModel,
                newItem: CommentsUiModel,
            ): Boolean {
                return (oldItem is CommentsUiModel.Comment && newItem is CommentsUiModel.Comment &&
                            oldItem.commentItem.content == newItem.commentItem.content)
            }

            override fun areContentsTheSame(
                oldItem: CommentsUiModel,
                newItem: CommentsUiModel,
            ): Boolean {
                return false
            }

        }

        private const val DEFAULT_AUTO_HIDE_MILLIS = 3000L
    }
}