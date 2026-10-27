package com.budzet.domain.budget.dto

import com.budzet.domain.budget.entity.BudgetChange
import com.budzet.domain.budget.entity.BudgetType
import java.time.LocalDateTime

data class BudgetChangeCreateResponse(
    val id: Long?,
    val requestId: Long?,
    val userId: Long?,
    val changeBudget: Long?,
    val changedBudget: Long?,
    val budgetType: BudgetType?,
    val userName: String?,
    val processedAt: LocalDateTime?,
    val reason: String?,
) {
    companion object {
        @JvmStatic
        fun from(budgetChange: BudgetChange): BudgetChangeCreateResponse = BudgetChangeCreateResponse(
            budgetChange.id, requireNotNull(budgetChange.request).id, budgetChange.user.id,
            budgetChange.changeBudget, budgetChange.changedBudget, budgetChange.type,
            budgetChange.userName, budgetChange.createdAt, budgetChange.reason,
        )
    }
}
