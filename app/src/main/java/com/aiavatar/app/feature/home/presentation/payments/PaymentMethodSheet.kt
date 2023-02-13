package com.aiavatar.app.feature.home.presentation.payments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.aiavatar.app.R
import com.aiavatar.app.core.fragment.BaseBottomSheetDialogFragment
import com.aiavatar.app.databinding.DialogPaymentMethodBinding
import com.aiavatar.app.databinding.ItemPaymentMethodBinding
import com.aiavatar.app.setOnSingleClickListener

class PaymentMethodSheet(
    private val paymentMethods: List<PaymentMethodData>,
    private val onItemClick: (data: PaymentMethodData) -> Unit
) : BaseBottomSheetDialogFragment(R.color.bottom_sheet_background) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_payment_method, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogPaymentMethodBinding.bind(view)

        binding.bindState()
    }

    private fun DialogPaymentMethodBinding.bindState() {

        val adapter = PaymentMethodsAdapter { position, data ->
            onItemClick.invoke(data)
            dismiss()
        }.apply {
            submitList(paymentMethods)
        }

        paymentMethodListView.adapter = adapter
    }

    companion object {
        const val TAG = "payment-methods-sheet"
    }
}

class PaymentMethodsAdapter(
    private val onItemClick: (position: Int, data: PaymentMethodData) -> Unit
) : ListAdapter<PaymentMethodData, PaymentMethodsAdapter.ItemVH>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        return ItemVH.from(parent)
    }

    override fun onBindViewHolder(holder: ItemVH, position: Int) {
        val model = getItem(position)
        holder.bind(model, onItemClick)
    }


    class ItemVH private constructor(
        private val binding: ItemPaymentMethodBinding
        ) : ViewHolder(binding.root) {

        fun bind(data: PaymentMethodData, onItemClick: (position: Int, data: PaymentMethodData) -> Unit) = with(binding) {
            tvItemTitle.text = data.title
            if (data.description != null && data.description.isNotBlank()) {
                tvItemDescription1.isVisible = true
                tvItemDescription1.text = data.description
            } else {
                tvItemDescription1.isVisible = false
            }

            if (data.brandLogo != null) {
                ivBrand.setImageResource(data.brandLogo)
                tvBrandSingleLetter.isVisible = false
            } else {
                // TODO: get brand name from drawable
                tvBrandSingleLetter.isVisible = true
                tvBrandSingleLetter.text = data.title[0].toString()
                ivBrand.setImageResource(R.color.signal_inverse_transparent_15)
            }

            root.setOnSingleClickListener(500) { onItemClick(adapterPosition, data) }
        }

        companion object {
            fun from(parent: ViewGroup): ItemVH {
                val itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_payment_method,
                    parent,
                    false
                )
                val binding = ItemPaymentMethodBinding.bind(itemView)
                return ItemVH(binding)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : ItemCallback<PaymentMethodData>() {
            override fun areContentsTheSame(oldItem: PaymentMethodData, newItem: PaymentMethodData): Boolean {
                return oldItem.paymentMethod == newItem.paymentMethod
            }

            override fun areItemsTheSame(oldItem: PaymentMethodData, newItem: PaymentMethodData): Boolean {
                return true
            }

        }
    }
}

data class PaymentMethodData(
    val paymentMethod: PaymentMethod,
    val title: String,
    val description: String? = null,
    @DrawableRes
    val brandLogo: Int? = null,
)

enum class PaymentMethod {
    IN_APP, OTHER
}