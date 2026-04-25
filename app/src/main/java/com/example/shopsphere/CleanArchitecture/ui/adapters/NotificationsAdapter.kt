package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.CleanArchitecture.data.local.notifications.NotificationEntity
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemNotificationBinding
import java.util.concurrent.TimeUnit

class NotificationsAdapter(
    private val onClick: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotificationsAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationEntity) {
            binding.textTitle.text = item.title
            binding.textBody.text = item.body
            binding.textTimestamp.text = formatTimestamp(item.timestamp)
            binding.unreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE
            val iconRes = resolveIconRes(item.iconName)
            binding.iconNotification.setImageResource(iconRes)
            binding.iconNotification.setColorFilter(
                ContextCompat.getColor(binding.root.context, R.color.bright_green)
            )
            binding.root.setOnClickListener { onClick(item) }
        }

        private fun resolveIconRes(name: String): Int {
            val ctx = binding.root.context
            val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
            return if (id != 0) id else R.drawable.ic_notification
        }

        private fun formatTimestamp(ts: Long): String {
            val diff = System.currentTimeMillis() - ts
            if (diff < 0) return ""
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return when {
                minutes < 1 -> "just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> android.text.format.DateFormat
                    .format("MMM d, yyyy", ts).toString()
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificationEntity>() {
            override fun areItemsTheSame(
                oldItem: NotificationEntity,
                newItem: NotificationEntity
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: NotificationEntity,
                newItem: NotificationEntity
            ) = oldItem == newItem
        }
    }
}
