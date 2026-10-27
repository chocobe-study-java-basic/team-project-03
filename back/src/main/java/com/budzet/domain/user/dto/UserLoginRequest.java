package com.budzet.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Schema(example = "user1@example.com")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Schema(example = "12341234")
        String password
){
}
