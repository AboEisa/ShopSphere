package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemOrderBinding
import java.util.Locale

class OrdersAdapter(
    private val onTrackClicked: (OrderHistoryItem) -> Unit,
    private val onReviewClicked: (OrderHistoryItem) -> Unit
) : ListAdapter<OrderHistoryItem, OrdersAdapter.OrderViewHolder>(DIFF) {

    private var completedMode = false

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).orderId.hashCode().toLong()

    fun submitList(items: List<OrderHistoryItem>, completedMode: Boolean) {
        this.completedMode = completedMode
        super.submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderHistoryItem) {
            val context = itemView.context
            val dash = context.getString(R.string.order_value_pending)

            // Always show "Order #<id>" — the API gives us a real id
            binding.textOrderId.text =
                if (item.orderId.isBlank() || item.orderId == "PENDING") dash
                else context.getString(R.string.account_order_prefix, item.orderId)

            binding.textOrderDate.text = item.date.ifBlank { dash }

            // Total — straight from /MyOrders.totalAmount via the VM mapper
            binding.textOrderTotal.text =
                item.total.ifBlank { item.itemPrice.ifBlank { dash } }

            // Detail rows: payment status + driver from the API
            binding.textPaymentStatus.text =
                item.paymentStatus?.takeIf { it.isNotBlank() } ?: dash
            val driver = item.driverName?.takeIf { it.isNotBlank() }
            binding.textDriverName.text = driver ?: dash

            // Navigation to the full details screen is intentionally disabled
            // for now (per spec: "make it default as null"). The card and the
            // action button are non-interactive until the details screen is wired.
            binding.root.setOnClickListener(null)
            binding.root.isClickable = false
            binding.root.isFocusable = false

            if (completedMode) {
                binding.textOrderStatus.text = context.getString(R.string.orders_completed)
                binding.textOrderStatus.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_order_status_completed)
                binding.textOrderStatus.setTextColor(
                    ContextCompat.getColor(context, R.color.bright_green)
                )

                val hasReview = item.reviewRating > 0.0
                binding.buttonOrderAction.isGone = hasReview
                binding.layoutRatingChip.isVisible = hasReview

                if (hasReview) {
                    binding.textReviewRating.text = context.getString(
                        R.string.reviews_average_value_inline,
                        item.reviewRating
                    )
                } else {
                    binding.buttonOrderAction.text =
                        context.getString(R.string.orders_leave_review)
                    binding.buttonOrderAction.setOnClickListener { onReviewClicked(item) }
                }
            } else {
                binding.layoutRatingChip.isGone = true
                binding.buttonOrderAction.isVisible = true
                binding.buttonOrderAction.text =
                    context.getString(R.string.order_view_details)
                // Per spec, view-details navigation is null for now.
                binding.buttonOrderAction.setOnClickListener(null)

                val step = resolveStatusStep(item)
                binding.textOrderStatus.text = ongoingStatusLabel(item)
                binding.textOrderStatus.background =
                    ContextCompat.getDrawable(context, statusChipBg(step))
                binding.textOrderStatus.setTextColor(
                    ContextCompat.getColor(context, statusChipColor(step))
                )
            }
        }

        private fun statusChipBg(step: Int): Int = when (step) {
            3 -> R.drawable.bg_order_status_completed
            2 -> R.drawable.bg_status_shipped
            1 -> R.drawable.bg_status_processing
            else -> R.drawable.bg_order_status_neutral
        }

        private fun statusChipColor(step: Int): Int = when (step) {
            3 -> R.color.bright_green
            2 -> R.color.status_blue
            1 -> R.color.status_orange
            else -> R.color._808080
        }

        private fun ongoingStatusLabel(item: OrderHistoryItem): String {
            return when (resolveStatusStep(item)) {
                3 -> itemView.context.getString(R.string.orders_completed)
                2 -> itemView.context.getString(R.string.track_status_in_transit)
                1 -> itemView.context.getString(R.string.track_status_picked)
                else -> itemView.context.getString(R.string.track_status_packing)
            }
        }

        private fun resolveStatusStep(item: OrderHistoryItem): Int {
            val normalizedStatus = item.status
                .trim()
                .lowercase(Locale.ENGLISH)
                .replace("_", " ")
                .replace("-", " ")

            val derivedStep = when {
                normalizedStatus.contains("deliver") || normalizedStatus.contains("complete") -> 3
                normalizedStatus.contains("transit") ||
                    normalizedStatus.contains("shipping") ||
                    normalizedStatus.contains("shipped") ||
                    normalizedStatus.contains("out for delivery") -> 2
                normalizedStatus.contains("pick") || normalizedStatus.contains("dispatch") -> 1
                else -> 0
            }

            return maxOf(item.statusStep.coerceIn(0, 3), derivedStep)
        }

    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OrderHistoryItem>() {
            override fun areItemsTheSame(oldItem: OrderHistoryItem, newItem: OrderHistoryItem): Boolean =
                oldItem.orderId == newItem.orderId

            override fun areContentsTheSame(oldItem: OrderHistoryItem, newItem: OrderHistoryItem): Boolean =
                oldItem == newItem
        }
    }
}
