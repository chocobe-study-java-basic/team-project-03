package com.budzet.domain.room.dto

import com.budzet.domain.room.entity.Currency
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class RoomCreateRequest(
    @field:NotBlank(message = "모임 이름은 필수입니다.")
    @field:Size(max = 20, message = "모임 이름은 20자 이하여야 합니다.")
    val name: String? = null,

    @field:NotNull(message = "총 예산은 필수입니다.")
    @field:PositiveOrZero(message = "총 예산은 음수가 될 수 없습니다.")
    val totalBudget: Long? = null,

    @field:NotNull(message = "통화는 필수입니다.")
    val currency: Currency? = null,
)
