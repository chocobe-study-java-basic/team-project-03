package com.budzet.domain.notification.dto

import com.budzet.domain.notification.entity.Notification
import com.budzet.domain.notification.entity.NotificationType
import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val roomId: Long,
    val roomName: String,
    val type: NotificationType,
    val message: String,
    val targetId: Long,
    val createdAt: LocalDateTime,
    val readAt: LocalDateTime?,
) {
    companion object {
        fun from(notification: Notification) = NotificationResponse(
            id = checkNotNull(notification.id),
            roomId = checkNotNull(notification.room.id),
            roomName = notification.room.name,
            type = notification.type,
            message = notification.message,
            targetId = notification.targetId,
            createdAt = checkNotNull(notification.createdAt),
            readAt = notification.readAt,
        )
    }
}

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
    val nextPage: Int?,
)

data class UnreadNotificationCountResponse(val count: Long)
