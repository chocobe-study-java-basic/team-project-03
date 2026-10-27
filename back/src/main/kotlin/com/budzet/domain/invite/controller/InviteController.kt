package com.budzet.domain.invite.controller

import com.budzet.domain.invite.dto.InviteJoinResponse
import com.budzet.domain.invite.dto.InviteResponse
import com.budzet.domain.invite.service.InviteService
import com.budzet.global.api.ApiResponse
import com.budzet.global.rq.Rq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "04. 초대", description = "초대 링크 생성, 목록 조회, 코드 참여 및 유효성 검증 API")
@RestController
@RequestMapping("/rooms")
class InviteController(
    private val inviteService: InviteService,
    private val rq: Rq,
) {
    @Operation(
        summary = "초대 링크 생성",
        description = "모임장 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @PostMapping("/{roomId}/invites")
    fun createInvite(@PathVariable roomId: Long): ApiResponse<InviteResponse> {
        val userId = requireNotNull(rq.getActor().id)
        val response = inviteService.createInvite(roomId, userId)
        return ApiResponse.success(HttpStatus.CREATED, "초대 링크 생성 성공", response)
    }

    @Operation(
        summary = "초대 링크 목록 조회",
        description = "모임장 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @GetMapping("/{roomId}/invites")
    fun getInvites(@PathVariable roomId: Long): ApiResponse<List<InviteResponse>> {
        val response = inviteService.getInvites(roomId, requireNotNull(rq.getActor().id))
        return ApiResponse.success(HttpStatus.OK, "초대 링크 조회 성공", response)
    }

    @Operation(summary = "초대 코드로 모임 참여", security = [SecurityRequirement(name = "BearerAuth")])
    @PostMapping("/join/{code}")
    fun joinRoom(@PathVariable code: String): ApiResponse<InviteJoinResponse> {
        val userId = requireNotNull(rq.getActor().id)
        val response = inviteService.joinRoom(code, userId)
        return ApiResponse.success(HttpStatus.CREATED, "모임에 참여하였습니다.", response)
    }
}
