package com.budzet.domain.user.dto

import jakarta.validation.constraints.NotBlank

data class UserProfileUpdateRequest(
    @field:NotBlank(message = "이름을 입력해주세요.")
    val name: String,
)
