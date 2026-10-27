package com.budzet.domain.budget.dto

import com.budzet.domain.budget.entity.BudgetRequestType
import java.time.LocalDateTime

data class BudgetRequestResponse(
    val id: Long?,
    val roomId: Long?,
    val userId: Long?,
    val userName: String?,
    val reason: String?,
    val requestedAmount: Long?,
    val status: BudgetRequestType?,
    val rejectReason: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
