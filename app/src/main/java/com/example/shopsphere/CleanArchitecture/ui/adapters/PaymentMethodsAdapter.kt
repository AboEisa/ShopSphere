package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.CleanArchitecture.ui.models.PaymentMethodItem
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemPaymentMethodBinding

class PaymentMethodsAdapter(
    private val onPaymentClicked: (PaymentMethodItem) -> Unit
) : RecyclerView.Adapter<PaymentMethodsAdapter.PaymentViewHolder>() {

    private val paymentMethods = mutableListOf<PaymentMethodItem>()

    fun submitList(items: List<PaymentMethodItem>) {
        paymentMethods.clear()
        paymentMethods.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentMethodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(paymentMethods[position])
    }

    override fun getItemCount(): Int = paymentMethods.size

    inner class PaymentViewHolder(private val binding: ItemPaymentMethodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PaymentMethodItem) {
            binding.textCardBrand.text = item.brand
            binding.textCardNumber.text =
                itemView.context.getString(R.string.account_card_ending_in, item.lastFour)
            binding.textDefaultBadge.visibility =
                if (item.isDefault) android.view.View.VISIBLE else android.view.View.GONE

            val rootBackground = if (item.isSelected) {
                R.drawable.bg_wallet_card_selected
            } else {
                R.drawable.bg_wallet_card
            }
            binding.rootCard.background = ContextCompat.getDrawable(itemView.context, rootBackground)

            val indicatorDrawable = if (item.isSelected) {
                R.drawable.bg_select_indicator_selected
            } else {
                R.drawable.bg_select_indicator
            }
            binding.selectIndicator.setImageDrawable(
                ContextCompat.getDrawable(itemView.context, indicatorDrawable)
            )

            binding.rootCard.setOnClickListener { onPaymentClicked(item) }
        }
    }
}
