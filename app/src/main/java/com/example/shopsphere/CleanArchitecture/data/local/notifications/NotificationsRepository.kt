package com.example.shopsphere.CleanArchitecture.data.local.notifications

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over [NotificationDao] with built-in dedupe so order-status
 * transitions that fire repeatedly (poll, onResume) don't spam the feed.
 */
@Singleton
class NotificationsRepository @Inject constructor(
    private val dao: NotificationDao
) {
    fun observe(): Flow<List<NotificationEntity>> = dao.observeAll()

    fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    /**
     * Inserts a notification unless an identical title+body was created within
     * the last [dedupeWindowMs] milliseconds (default 60s).
     */
    suspend fun notify(
        title: String,
        body: String,
        deepLink: String? = null,
        iconName: String = "ic_notification",
        dedupeWindowMs: Long = 60_000L
    ): Long {
        val since = System.currentTimeMillis() - dedupeWindowMs
        val duplicates = dao.countRecentDuplicates(title, body, since)
        if (duplicates > 0) return -1L
        return dao.insert(
            NotificationEntity(
                title = title,
                body = body,
                deepLink = deepLink,
                iconName = iconName
            )
        )
    }

    suspend fun markRead(id: Long) = dao.markAsRead(id)
    suspend fun markAllRead() = dao.markAllRead()
    suspend fun clear() = dao.clear()
}
