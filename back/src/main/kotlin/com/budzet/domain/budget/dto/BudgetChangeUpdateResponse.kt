package com.budzet.domain.budget.dto

import com.budzet.domain.budget.entity.BudgetChange

data class BudgetChangeUpdateResponse(
    val id: Long?,
    val changeBudget: Long?,
    val changedBudget: Long?,
    val userName: String?,
    val userEmail: String?,
    val reason: String?,
) {
    companion object {
        @JvmStatic
        fun from(budgetChange: BudgetChange): BudgetChangeUpdateResponse = BudgetChangeUpdateResponse(
            budgetChange.id, budgetChange.changeBudget, budgetChange.changedBudget,
            budgetChange.userName, budgetChange.user.email, budgetChange.reason,
        )
    }
}
