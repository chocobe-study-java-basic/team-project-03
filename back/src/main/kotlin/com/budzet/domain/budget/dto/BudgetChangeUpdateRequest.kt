package com.budzet.domain.budget.dto

import jakarta.validation.constraints.*

data class BudgetChangeUpdateRequest(
    @field:NotNull(message = "신청 금액은 필수입니다.")
    @field:Positive(message = "정산금액은 음수 또는 0이 될 수 없습니다.")
    val changeBudget: Long? = null,
    @field:NotNull(message = "정산 금액은 필수입니다.")
    @field:Positive(message = "정산금액은 음수 또는 0이 될 수 없습니다.")
    val changedBudget: Long? = null,
    @field:NotBlank(message = "유저 이름은 필수입니다.")
    val userName: String? = null,
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val userEmail: String? = null,
    @field:NotBlank(message = "예산 변동 사유는 필수입니다.")
    @field:Size(max = 20, message = "예산 변동 사유는 최대 20자까지 입력 가능합니다.")
    val reason: String? = null,
)
