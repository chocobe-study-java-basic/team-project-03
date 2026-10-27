package com.budzet.domain.room.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RoomUpdateRequest(
    @field:NotBlank(message = "모임 이름은 필수입니다.")
    @field:Size(max = 20, message = "모임 이름은 20자 이하여야 합니다.")
    val name: String? = null,
)
