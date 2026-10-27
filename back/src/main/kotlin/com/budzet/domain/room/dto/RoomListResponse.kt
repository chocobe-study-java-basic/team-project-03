package com.budzet.domain.room.dto

import com.budzet.domain.room.entity.Room
import java.time.LocalDateTime

data class RoomListResponse(
    val rooms: List<RoomItem>,
) {
    companion object {
        @JvmStatic
        fun from(rooms: List<Room>): RoomListResponse = RoomListResponse(
            rooms = rooms.map {
                RoomItem(
                    id = it.id,
                    name = it.name,
                    totalBudget = it.totalBudget,
                    availableBudget = it.availableBudget,
                    currency = it.currency.name,
                    createdAt = it.createdAt,
                )
            },
        )
    }

    data class RoomItem(
        val id: Long?,
        val name: String,
        val totalBudget: Long,
        val availableBudget: Long,
        val currency: String,
        val createdAt: LocalDateTime?,
    )
}
