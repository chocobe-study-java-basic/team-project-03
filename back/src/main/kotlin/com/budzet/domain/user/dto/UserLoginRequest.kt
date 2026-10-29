package com.budzet.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserLoginRequest(
    @field:NotBlank(message = "이메일을 입력해주세요.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    @field:Schema(example = "user1@example.com")
    val email: String?,

    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    @field:Schema(example = "12341234")
    val password: String?,
)
