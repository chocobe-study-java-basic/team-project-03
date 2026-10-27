package com.budzet.domain.notification.entity

import com.budzet.domain.room.entity.Room
import com.budzet.domain.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(indexes = [
    Index(name = "idx_notification_recipient_id", columnList = "recipient_id,id"),
    Index(name = "idx_notification_recipient_read", columnList = "recipient_id,read_at"),
])
@EntityListeners(AuditingEntityListener::class)
open class Notification(
    recipient: User,
    room: Room,
    type: NotificationType,
    message: String,
    targetId: Long,
) {
    init {
        require(message.isNotBlank() && message.length <= 200) { "알림 메시지는 1~200자여야 합니다." }
        require(targetId > 0) { "알림 대상 ID는 양수여야 합니다." }
    }

    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY, optional = false)
    @field:JoinColumn(name = "recipient_id", nullable = false)
    @field:OnDelete(action = OnDeleteAction.CASCADE)
    open var recipient: User = recipient
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY, optional = false)
    @field:JoinColumn(name = "room_id", nullable = false)
    @field:OnDelete(action = OnDeleteAction.CASCADE)
    open var room: Room = room
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false, length = 30)
    open var type: NotificationType = type
        protected set

    @field:Column(nullable = false, length = 200)
    open var message: String = message
        protected set

    @field:Column(nullable = false)
    open var targetId: Long = targetId
        protected set

    @field:CreatedDate
    @field:Column(nullable = false, updatable = false)
    open var createdAt: LocalDateTime? = null
        protected set

    @field:Column(name = "read_at")
    open var readAt: LocalDateTime? = null
        protected set

    fun markAsRead(now: LocalDateTime = LocalDateTime.now()) {
        if (readAt == null) readAt = now
    }
}
