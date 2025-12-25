package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemOrderBinding
import java.util.Locale

class OrdersAdapter(
    private val onOrderClicked: (OrderHistoryItem) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    private val orders = mutableListOf<OrderHistoryItem>()

    fun submitList(items: List<OrderHistoryItem>) {
        orders.clear()
        orders.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(private val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderHistoryItem) {
            binding.textOrderId.text = itemView.context.getString(R.string.account_order_prefix, item.orderId)
            binding.textOrderDate.text = item.date
            binding.textOrderTotal.text = item.total
            binding.textOrderStatus.text = item.status

            when (item.status.lowercase(Locale.ENGLISH)) {
                "delivered" -> {
                    binding.textOrderStatus.background =
                        ContextCompat.getDrawable(itemView.context, R.drawable.bg_status_delivered)
                    binding.textOrderStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.bright_green)
                    )
                }

                "shipped" -> {
                    binding.textOrderStatus.background =
                        ContextCompat.getDrawable(itemView.context, R.drawable.bg_status_shipped)
                    binding.textOrderStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.status_blue)
                    )
                }

                "picked", "in transit" -> {
                    binding.textOrderStatus.background =
                        ContextCompat.getDrawable(itemView.context, R.drawable.bg_status_shipped)
                    binding.textOrderStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.status_blue)
                    )
                }

                else -> {
                    binding.textOrderStatus.background =
                        ContextCompat.getDrawable(itemView.context, R.drawable.bg_status_processing)
                    binding.textOrderStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.status_orange)
                    )
                }
            }

            binding.root.setOnClickListener { onOrderClicked(item) }
        }
    }
}
