package com.budzet.domain.budget.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class BudgetRequestModifyRequest(
    @field:NotBlank(message = "신청사유는 필수입니다.")
    @field:Size(max = 20, message = "신청사유는 20자 이하여야 합니다.")
    val reason: String = "",
    @field:NotNull(message = "신청예산은 필수입니다.")
    @field:Positive(message = "신청예산은 음수 또는 0이 될 수 없습니다.")
    val rerequestedAmount: Long? = null
)
