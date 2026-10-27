package com.budzet.domain.room.entity

import java.io.Serializable

data class UserRoomConnectionId(
    private var user: Long? = null,
    private var room: Long? = null,
) : Serializable
