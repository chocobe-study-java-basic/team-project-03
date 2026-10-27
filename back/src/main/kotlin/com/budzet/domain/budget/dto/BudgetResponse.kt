package com.budzet.domain.budget.dto

import com.budzet.domain.room.entity.Room

data class BudgetResponse(
    val roomId: Long,
    val roomName: String,
    val totalBudget: Long,
    val availableBudget: Long,
    val reserveBudget: Long,
    val currency: String,
) {
    companion object {
        @JvmStatic
        fun from(room: Room): BudgetResponse {
            val totalBudget = requireNotNull(room.totalBudget)
            val availableBudget = requireNotNull(room.availableBudget)
            return BudgetResponse(
                requireNotNull(room.id), room.name, totalBudget, availableBudget,
                totalBudget - availableBudget, room.currency.name,
            )
        }
    }
}
