package com.budzet.domain.user.controller

import com.budzet.domain.user.dto.*
import com.budzet.domain.user.service.UserService
import com.budzet.global.api.ApiResponse
import com.budzet.global.exception.BusinessException
import com.budzet.global.exception.ErrorCode
import com.budzet.global.rq.Rq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

@Tag(name = "01. 회원 & 인증", description = "회원가입, 로그인/로그아웃, 내 정보 조회 및 토큰 재발급 API")
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val rq: Rq,
    @Value("\${custom.jwt.access-expire-millis}")
    private val accessExpireMillis: Long,
    @Value("\${custom.jwt.refresh-expire-millis}")
    private val refreshExpireMillis: Long
) {
    @Operation(summary = "회원가입")
    @PostMapping("/join")
    fun join(@RequestBody @Valid reqBody: UserJoinRequest): ResponseEntity<ApiResponse<UserDto>> {
        val user = userService.join(
            requireNotNull(reqBody.email),
            requireNotNull(reqBody.password),
            requireNotNull(reqBody.name),
        )
        return ApiResponse.response(HttpStatus.CREATED, "회원가입이 완료되었습니다.", UserDto(user))
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호를 확인하여 로그인")
    @PostMapping("/login")
    fun login(@RequestBody @Valid reqBody: UserLoginRequest): ResponseEntity<ApiResponse<UserLoginResponse>> {
        val response = userService.login(requireNotNull(reqBody.email), requireNotNull(reqBody.password))

        rq.addCookie("accessToken", response.accessToken, Duration.ofMillis(accessExpireMillis))
        rq.addCookie("refreshToken", response.refreshToken, Duration.ofMillis(refreshExpireMillis))

        return ApiResponse.response(HttpStatus.OK, "로그인에 성공했습니다.", response)
    }

    @Operation(summary = "로그아웃", security = [SecurityRequirement(name = "BearerAuth")])
    @DeleteMapping("/logout")
    fun logout(): ResponseEntity<ApiResponse<Void>> {
        userService.logout(rq.getActor().id)

        rq.deleteCookie("accessToken")
        rq.deleteCookie("refreshToken")

        return ApiResponse.response(HttpStatus.OK, "로그아웃되었습니다.", null)
    }

    @Operation(summary = "인증 토큰 재발급", description = "access token과 refresh token을 재발급")
    @PostMapping("/refresh")
    fun refresh(): ResponseEntity<ApiResponse<TokenRefreshResponse>> {
        val headerAuthorization = rq.getHeader("Authorization", "")

        val refreshToken = if (headerAuthorization.isNotBlank()) {
            if (!headerAuthorization.startsWith("Bearer ")) {
                throw BusinessException(ErrorCode.INVALID_AUTHORIZATION_HEADER)
            }
            headerAuthorization.substring(7)
        } else {
            rq.getCookieValue("refreshToken", "")
        }

        if (refreshToken.isBlank()) {
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        }
        val tokenResponse = userService.refresh(refreshToken)

        rq.addCookie("accessToken", tokenResponse.accessToken, Duration.ofMillis(accessExpireMillis))
        rq.addCookie("refreshToken", tokenResponse.refreshToken, Duration.ofMillis(refreshExpireMillis))

        return ApiResponse.response(HttpStatus.OK, "토큰 재발급 성공", tokenResponse)
    }

    @Operation(summary = "내 정보 조회", security = [SecurityRequirement(name = "BearerAuth")])
    @GetMapping("/me")
    fun me(): ResponseEntity<ApiResponse<UserDto>> =
        ApiResponse.response(HttpStatus.OK, "내 정보 조회에 성공했습니다.", UserDto(rq.getActor()))

    @Operation(
        summary = "비밀번호 변경",
        description = "현재 비밀번호 확인 후 변경",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @PatchMapping("/me/password")
    fun updatePassword(@RequestBody @Valid reqBody: UserPasswordUpdateRequest): ResponseEntity<ApiResponse<Void>> {
        userService.updatePassword(
            rq.getActor().id,
            requireNotNull(reqBody.currentPassword),
            requireNotNull(reqBody.newPassword),
        )
        rq.deleteCookie("accessToken")
        rq.deleteCookie("refreshToken")
        return ApiResponse.response(HttpStatus.OK, "비밀번호가 변경되었습니다. 다시 로그인해주세요.", null)
    }

    @Operation(summary = "내 정보 수정", security = [SecurityRequirement(name = "BearerAuth")])
    @PatchMapping("/me")
    fun updateProfile(@RequestBody @Valid reqBody: UserProfileUpdateRequest): ResponseEntity<ApiResponse<UserDto>> {
        val updatedUser = userService.updateProfile(rq.getActor().id, requireNotNull(reqBody.name))
        return ApiResponse.response(HttpStatus.OK, "내 정보가 수정되었습니다.", UserDto(updatedUser))
    }
}
