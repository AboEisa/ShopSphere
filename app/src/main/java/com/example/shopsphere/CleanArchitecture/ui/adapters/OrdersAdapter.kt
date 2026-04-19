package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
            binding.textOrderId.text = item.itemTitle.ifBlank {
                context.getString(R.string.account_order_prefix, item.orderId)
            }
            val hasSize = item.itemSize.isNotBlank()
            binding.textOrderDate.isVisible = hasSize
            if (hasSize) {
                binding.textOrderDate.text = context.getString(
                    R.string.orders_size_value,
                    item.itemSize
                )
            }
            binding.textOrderTotal.text = item.itemPrice.ifBlank { item.total }

            Glide.with(binding.imageOrderProduct)
                .load(item.itemImageUrl)
                .placeholder(R.drawable.ic_order)
                .error(R.drawable.ic_order)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imageOrderProduct)

            if (completedMode) {
                binding.textOrderStatus.text = context.getString(R.string.orders_completed)
                binding.textOrderStatus.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_order_status_completed)
                binding.textOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.bright_green))

                val hasReview = item.reviewRating > 0.0
                binding.buttonOrderAction.isGone = hasReview
                binding.layoutRatingChip.isVisible = hasReview

                if (hasReview) {
                    binding.textReviewRating.text = context.getString(
                        R.string.reviews_average_value_inline,
                        item.reviewRating
                    )
                    binding.root.setOnClickListener(null)
                } else {
                    styleBlackButton(binding.buttonOrderAction)
                    binding.buttonOrderAction.text = context.getString(R.string.orders_leave_review)
                    binding.buttonOrderAction.setOnClickListener { onReviewClicked(item) }
                    binding.root.setOnClickListener { onReviewClicked(item) }
                }
            } else {
                binding.layoutRatingChip.isGone = true
                binding.buttonOrderAction.isVisible = true
                styleBlackButton(binding.buttonOrderAction)
                binding.buttonOrderAction.text = context.getString(R.string.orders_track_order)
                binding.buttonOrderAction.setOnClickListener { onTrackClicked(item) }
                binding.root.setOnClickListener { onTrackClicked(item) }

                binding.textOrderStatus.text = ongoingStatusLabel(item)
                binding.textOrderStatus.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_order_status_neutral)
                binding.textOrderStatus.setTextColor(ContextCompat.getColor(context, R.color._808080))
            }
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

        private fun styleBlackButton(button: com.google.android.material.button.MaterialButton) {
            button.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_black_action)
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
