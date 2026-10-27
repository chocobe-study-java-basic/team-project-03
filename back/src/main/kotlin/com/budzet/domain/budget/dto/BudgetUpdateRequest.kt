package com.budzet.domain.budget.dto

import com.budzet.domain.budget.entity.BudgetType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class BudgetUpdateRequest(
    @field:NotNull(message = "예산 변경 금액 입력은 필수입니다.")
    @field:Positive(message = "예산 변경 금액은 음수 또는 0이 될 수 없습니다.")
    val totalBudget: Long? = null,
    @field:NotNull(message = "예산 변경 타입은 필수입니다.")
    val budgetType: BudgetType? = null,
    @field:Size(max = 20, message = "변동 사유는 최대 20자까지 입력 가능합니다.")
    val reason: String? = null,
)
