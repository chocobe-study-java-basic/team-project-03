package com.budzet.domain.room.dto

import com.budzet.domain.room.entity.Room
import java.time.LocalDateTime

data class RoomDetailResponse(
    val id: Long?,
    val name: String,
    val totalBudget: Long,
    val availableBudget: Long,
    val currency: String,
    val createdAt: LocalDateTime?,
) {
    companion object {
        @JvmStatic
        fun from(room: Room): RoomDetailResponse = RoomDetailResponse(
            id = room.id,
            name = room.name,
            totalBudget = requireNotNull(room.totalBudget),
            availableBudget = requireNotNull(room.availableBudget),
            currency = room.currency.name,
            createdAt = room.createdAt,
        )
    }
}
