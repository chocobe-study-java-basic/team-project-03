package com.budzet.domain.invite.dto

import com.budzet.domain.room.entity.UserRoomConnection
import java.time.LocalDateTime

data class InviteJoinResponse(
    val userId: Long?,
    val roomId: Long?,
    val authority: String,
    val joined: Boolean,
    val createdAt: LocalDateTime?,
) {
    companion object {
        @JvmStatic
        fun from(connection: UserRoomConnection): InviteJoinResponse = InviteJoinResponse(
            userId = connection.user.id,
            roomId = connection.room.id,
            authority = connection.authority.name,
            joined = connection.joined,
            createdAt = connection.createdAt,
        )
    }
}
