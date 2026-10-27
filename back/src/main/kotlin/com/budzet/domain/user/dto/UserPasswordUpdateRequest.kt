package com.budzet.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserPasswordUpdateRequest(
    @field:NotBlank(message = "현재 비밀번호를 입력해주세요.")
    val currentPassword: String? = null,

    @field:NotBlank(message = "새 비밀번호를 입력해주세요.")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    val newPassword: String? = null,
)
