package com.example.shopsphere.CleanArchitecture.data.local.notifications

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted in-app notification. Seeded by ViewModel events (order status change,
 * checkout success, chat replies) and rendered by NotificationsFragment.
 *
 * `deepLink` is an optional navigation payload. Supported values:
 *   - "track_order:<orderId>"  → navigate to trackOrderFragment with orderId
 *   - "chatbot"                → navigate to chatBotFragment
 *   - null / ""                → no navigation on tap
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val deepLink: String? = null,
    /** Drawable res name (e.g. "ic_bell", "ic_box") looked up via resources. */
    val iconName: String = "ic_notification"
)
