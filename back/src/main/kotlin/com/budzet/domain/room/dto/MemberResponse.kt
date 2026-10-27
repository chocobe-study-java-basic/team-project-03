package com.budzet.domain.room.dto

import com.budzet.domain.room.entity.Authority
import com.budzet.domain.room.entity.UserRoomConnection

data class MemberResponse(
    val userId: Long?,
    val name: String,
    val authority: Authority,
) {
    companion object {
        @JvmStatic
        fun from(connection: UserRoomConnection): MemberResponse = MemberResponse(
            userId = connection.user.id,
            name = connection.user.name,
            authority = connection.authority,
        )
    }
}
