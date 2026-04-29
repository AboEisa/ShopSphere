package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemOrderBinding
import java.util.Locale

class OrdersAdapter(
    private val onTrackClicked: (OrderHistoryItem) -> Unit,
    private val onReviewClicked: (OrderHistoryItem) -> Unit,
    private val onOrderClicked: (OrderHistoryItem) -> Unit,
    private val onPayAgainClicked: ((OrderHistoryItem) -> Unit)? = null
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

            // Top row: ORDER #1004 (caps, muted) + status pill
            val orderId = item.orderId.takeIf { it.isNotBlank() && it != "PENDING" }
            binding.textOrderId.text = orderId
                ?.let { context.getString(R.string.account_order_prefix, it).uppercase(Locale.ENGLISH) }
                ?: dash

            // Date row — "Placed on Oct 24, 2023" or "Delivered on …" for delivered
            val step = resolveStatusStep(item)
            val dateText = item.date.ifBlank { dash }
            binding.textOrderDate.text = if (step >= 3) {
                context.getString(R.string.orders_delivered_on, dateText)
            } else {
                context.getString(R.string.orders_placed_on, dateText)
            }

            // Thumbnails: show all product images from the order
            renderProductThumbnails(item, context)

            // Total amount in green
            binding.textOrderTotal.text = item.total.ifBlank { item.itemPrice.ifBlank { dash } }

            // Status pill — color depends on the step
            renderStatusPill(item, step, context)

            // Card root click handler - navigate to order details
            binding.orderCard.setOnClickListener {
                onOrderClicked(item)
            }
            binding.orderCard.isClickable = true

            // Action button morphs based on the order step:
            //  - delivered  -> "Buy Again" (light green pill)
            //  - in transit -> "Track Order" (solid green)
            //  - otherwise  -> "Order Details" (light grey pill)
            renderActionButton(item, step, context)
        }

        private fun renderProductThumbnails(item: OrderHistoryItem, context: android.content.Context) {
            val products = item.products

            if (products.isEmpty()) {
                // No products - show placeholder
                binding.thumbnail1.isVisible = true
                binding.thumbnail1Image.setImageResource(R.drawable.ic_order)
                binding.thumbnail1Image.setColorFilter(0xFF94A3B8.toInt())
                binding.thumbnail2.isGone = true
                binding.thumbnailOverflow.isGone = true
                return
            }

            // Show first product image
            binding.thumbnail1.isVisible = true
            loadProductImage(binding.thumbnail1Image, products[0].imageUrl)

            // Show second product image or overflow
            if (products.size > 1) {
                if (products.size == 2) {
                    // Show second image
                    binding.thumbnail2.isVisible = true
                    binding.thumbnailOverflow.isGone = true
                    loadProductImage(binding.thumbnail2Image, products[1].imageUrl)
                } else {
                    // Show overflow count (+2, +3, etc.)
                    binding.thumbnail2.isGone = true
                    binding.thumbnailOverflow.isVisible = true
                    binding.thumbnailOverflow.text = "+${products.size - 1}"
                }
            } else {
                binding.thumbnail2.isGone = true
                binding.thumbnailOverflow.isGone = true
            }
        }

        private fun loadProductImage(imageView: ImageView, imageUrl: String?) {
            val context = imageView.context
            android.util.Log.d("OrdersAdapter", "Loading image: $imageUrl")

            if (!imageUrl.isNullOrEmpty()) {
                // Clear any tint before loading real image
                imageView.colorFilter = null
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_order)
                    .error(R.drawable.ic_order)
                    .centerInside()
                    .into(imageView)
                android.util.Log.d("OrdersAdapter", "✅ Loading real image")
            } else {
                imageView.setImageResource(R.drawable.ic_order)
                // Apply grey tint to placeholder
                imageView.setColorFilter(0xFF94A3B8.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
                android.util.Log.d("OrdersAdapter", "⚠️ No image URL, showing placeholder")
            }
        }
        private fun renderStatusPill(item: OrderHistoryItem, step: Int, context: android.content.Context) {
            binding.textOrderStatus.text = statusLabel(item, step, context)

            val (bgRes, textColorInt) = when (step) {
                3 -> R.drawable.bg_order_status_completed to ContextCompat.getColor(context, R.color.bright_green)
                2 -> R.drawable.bg_order_status_shipped to ContextCompat.getColor(context, R.color.bright_green)
                1 -> R.drawable.bg_order_status_processing to 0xFF1F4F7C.toInt()
                else -> R.drawable.bg_order_status_processing to 0xFF1F4F7C.toInt()
            }
            binding.textOrderStatus.background = ContextCompat.getDrawable(context, bgRes)
            binding.textOrderStatus.setTextColor(textColorInt)
        }

        private fun renderActionButton(
            item: OrderHistoryItem,
            step: Int,
            context: android.content.Context
        ) {
            val (bgRes, label, textColor) = when {
                step >= 3 -> Triple(
                    R.drawable.bg_order_buyagain_button,
                    context.getString(R.string.orders_buy_again),
                    ContextCompat.getColor(context, R.color.bright_green)
                )
                step == 2 -> Triple(
                    R.drawable.bg_order_track_button,
                    context.getString(R.string.orders_track_order),
                    0xFFFFFFFF.toInt()
                )
                else -> Triple(
                    R.drawable.bg_order_details_button,
                    context.getString(R.string.order_view_details),
                    0xFF1F2937.toInt()
                )
            }
            binding.buttonOrderAction.background = ContextCompat.getDrawable(context, bgRes)
            binding.buttonOrderAction.text = label
            binding.buttonOrderAction.setTextColor(textColor)

            // Reviews UI (completed mode + already reviewed) — keep the chip path
            // but only when an existing review exists. The action button still
            // shows "Buy Again" for delivered orders.
            val hasReview = item.reviewRating > 0.0
            if (completedMode && hasReview) {
                binding.layoutRatingChip.isVisible = true
                binding.textReviewRating.text = context.getString(
                    R.string.reviews_average_value_inline,
                    item.reviewRating
                )
            } else {
                binding.layoutRatingChip.isGone = true
            }

            // Per spec: every action button is a no-op until the details screen
            // is wired. Leaving review submission still works through long-press
            // / details flow, not the card itself.
            binding.buttonOrderAction.setOnClickListener(null)
            binding.buttonOrderAction.isClickable = false

            // "Pay Again" button — visible only for orders with pending/failed payment
            val paymentStatus = item.paymentStatus?.lowercase(Locale.ENGLISH).orEmpty()
            val needsPayment = paymentStatus == "pending" || paymentStatus == "failed" || paymentStatus == "unpaid"
            if (needsPayment && onPayAgainClicked != null) {
                binding.buttonPayAgain.visibility = android.view.View.VISIBLE
                binding.buttonPayAgain.setOnClickListener { onPayAgainClicked.invoke(item) }
            } else {
                binding.buttonPayAgain.visibility = android.view.View.GONE
            }
        }

        private fun statusLabel(
            item: OrderHistoryItem,
            step: Int,
            context: android.content.Context
        ): String {
            // Use backend status if available, otherwise fall back to step-based
            return item.status.ifBlank {
                when (step) {
                    3 -> context.getString(R.string.orders_status_delivered)
                    2 -> context.getString(R.string.orders_status_transit)
                    1 -> context.getString(R.string.orders_status_picked)
                    else -> context.getString(R.string.orders_status_packing)
                }
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