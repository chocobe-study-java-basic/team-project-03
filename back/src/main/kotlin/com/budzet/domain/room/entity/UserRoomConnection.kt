package com.budzet.domain.room.entity

import com.budzet.domain.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@IdClass(UserRoomConnectionId::class)
@EntityListeners(AuditingEntityListener::class)
open class UserRoomConnection private constructor(
    user: User,
    room: Room,
    authority: Authority,
    joined: Boolean,
) {
    @field:Id
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "user_id")
    open var user: User = user
        protected set

    @field:Id
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "room_id")
    open var room: Room = room
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(length = 20, nullable = false)
    open var authority: Authority = authority
        protected set

    open var joined: Boolean = joined
        protected set

    @field:CreatedDate
    open var createdAt: LocalDateTime? = null
        protected set

    open fun isJoined(): Boolean = joined

    fun changeAuthority(authority: Authority) {
        this.authority = authority
    }

    companion object {
        @JvmStatic
        fun createOwner(user: User, room: Room): UserRoomConnection =
            UserRoomConnection(user, room, Authority.OWNER, true)

        @JvmStatic
        fun createMember(user: User, room: Room): UserRoomConnection =
            UserRoomConnection(user, room, Authority.MEMBER, true)
    }
}
