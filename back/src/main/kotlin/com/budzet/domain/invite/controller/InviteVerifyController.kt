package com.budzet.domain.invite.controller

import com.budzet.domain.invite.dto.InviteResponse
import com.budzet.domain.invite.service.InviteService
import com.budzet.global.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "04. 초대", description = "초대 링크 생성, 목록 조회, 코드 참여 및 유효성 검증 API")
@RestController
@RequestMapping("/invites")
class InviteVerifyController(
    private val inviteService: InviteService,
) {
    @Operation(summary = "초대 링크 검증")
    @GetMapping("/{token}")
    fun verifyInvite(@PathVariable token: String): ApiResponse<InviteResponse> {
        val response = inviteService.verifyInvite(token)
        return ApiResponse.success(HttpStatus.OK, "초대 링크 검증 성공", response)
    }
}
