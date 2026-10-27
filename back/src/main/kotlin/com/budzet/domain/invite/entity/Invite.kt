package com.budzet.domain.invite.entity

import com.budzet.domain.room.entity.Room
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
open class Invite(code: String, room: Room, expireAt: LocalDateTime) {
    @field:Id
    @field:Column(length = 10)
    open var code: String = code
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "room_id")
    open var room: Room = room
        protected set

    @field:CreatedDate
    open var createdAt: LocalDateTime? = null
        protected set

    open var expireAt: LocalDateTime = expireAt
        protected set
}
