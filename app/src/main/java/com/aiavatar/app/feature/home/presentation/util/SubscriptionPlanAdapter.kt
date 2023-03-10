package com.aiavatar.app.feature.home.presentation.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.R
import com.aiavatar.app.databinding.ItemSubscriptionPackageBinding
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.presentation.subscription.SubscriptionUiModel

class SubscriptionPlanAdapter(
    private val callback: Callback
) : ListAdapter<SubscriptionUiModel, ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> ItemViewHolder.from(parent)
            else -> throw IllegalStateException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = getItem(position)
        when (holder) {
            is ItemViewHolder -> {
                model as SubscriptionUiModel.Plan
                holder.bind(model.subscriptionPlan, model.selected, callback)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            if (isValidPayload(payloads)) {
                val bundle = (payloads.firstOrNull() as? Bundle) ?: kotlin.run {
                    super.onBindViewHolder(holder, position, payloads); return
                }
                if (bundle.containsKey(SELECTION_TOGGLE_PAYLOAD)) {
                    (holder as? ItemViewHolder)?.toggleSelection(bundle.getBoolean(
                        SELECTION_TOGGLE_PAYLOAD, false))
                }
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SubscriptionUiModel.Plan -> VIEW_TYPE_ITEM
            else -> throw IllegalStateException("Cannot decide a view type for position $position")
        }
    }

    private fun isValidPayload(payloads: MutableList<Any>?): Boolean {
        return (payloads?.firstOrNull() as? Bundle)?.keySet()?.any {
                    it == SELECTION_TOGGLE_PAYLOAD
        } ?: false
    }

    class ItemViewHolder private constructor(
        private val binding: ItemSubscriptionPackageBinding
        ) : ViewHolder(binding.root) {

        fun bind(data: SubscriptionPlan, selected: Boolean, callback: Callback) = with(binding) {
            textPrice.text = data.price
            textCurrencySymbol.text = data.currencySymbol
            textPhotos.text = binding.root.context.getString(R.string.num_photos, data.photo)
            description.text = binding.root.context.getString(R.string.subscription_description, data.variation, data.style)

            toggleSelection(selected)

            root.setOnClickListener {
                checkbox.isChecked = true
                // callback.onItemClick(adapterPosition)
            }
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                // callback.onSelectPlan(adapterPosition, data)
                if (isChecked) {
                    callback.onSelectPlan(adapterPosition, data)
                }
            }

            bestSellerContainer.isVisible = data.bestSeller
        }

        fun toggleSelection(selected: Boolean) = with(binding) {
            checkbox.isChecked = selected
            priceContainer.isSelected = selected
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_subscription_package,
                    parent,
                    false
                )
                val binding = ItemSubscriptionPackageBinding.bind(itemView)
                return ItemViewHolder(binding)
            }
        }
    }

    interface Callback {
        fun onItemClick(position: Int)
        fun onSelectPlan(position: Int, plan: SubscriptionPlan)
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0

        private const val SELECTION_TOGGLE_PAYLOAD = "selection_toggle"

        val DIFF_CALLBACK = object : ItemCallback<SubscriptionUiModel>() {
            override fun areItemsTheSame(
                oldItem: SubscriptionUiModel,
                newItem: SubscriptionUiModel,
            ): Boolean {
                return (oldItem is SubscriptionUiModel.Plan && newItem is SubscriptionUiModel.Plan &&
                        oldItem.subscriptionPlan.id == newItem.subscriptionPlan.id)
            }

            override fun areContentsTheSame(
                oldItem: SubscriptionUiModel,
                newItem: SubscriptionUiModel,
            ): Boolean {
                return (oldItem is SubscriptionUiModel.Plan && newItem is SubscriptionUiModel.Plan &&
                        oldItem.subscriptionPlan == newItem.subscriptionPlan && oldItem.selected == newItem.selected)
            }

            override fun getChangePayload(
                oldItem: SubscriptionUiModel,
                newItem: SubscriptionUiModel
            ): Any {
                val updatePayload = bundleOf()
                when {
                    oldItem is SubscriptionUiModel.Plan && newItem is SubscriptionUiModel.Plan -> {
                        if (oldItem.selected != newItem.selected) {
                            updatePayload.putBoolean(SELECTION_TOGGLE_PAYLOAD, newItem.selected)
                        }
                    }
                }
                return updatePayload
            }

        }
    }
}