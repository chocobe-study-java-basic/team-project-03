package com.budzet.domain.room.controller

import com.budzet.domain.room.dto.MemberResponse
import com.budzet.domain.room.service.UserRoomConnectionService
import com.budzet.global.api.ApiResponse
import com.budzet.global.rq.Rq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "02 - 2. 멤버", description = "모임방 멤버 권한 조회/변경, 목록 조회, 강퇴 및 탈퇴 API")
@RestController
@RequestMapping("/rooms")
class UserRoomConnectionController(
    private val userRoomConnectionService: UserRoomConnectionService,
    private val rq: Rq,
) {
    @Operation(summary = "멤버 권한 조회", security = [SecurityRequirement(name = "BearerAuth")])
    @GetMapping("/{roomId}/members/{userId}/authority")
    fun getAuthority(@PathVariable roomId: Long, @PathVariable userId: Long): ApiResponse<String> {
        val authority = userRoomConnectionService
            .getConnection(roomId, requireNotNull(rq.getActor().id), userId)
            .authority.name
        return ApiResponse.success(HttpStatus.OK, "멤버 권한 조회 성공", authority)
    }

    @Operation(summary = "멤버 목록 조회", security = [SecurityRequirement(name = "BearerAuth")])
    @GetMapping("/{roomId}/members")
    fun getMembers(@PathVariable roomId: Long): ApiResponse<List<MemberResponse>> {
        val members = userRoomConnectionService.getMembers(roomId, requireNotNull(rq.getActor().id))
        return ApiResponse.success(HttpStatus.OK, "멤버 목록 조회 성공", members)
    }

    @Operation(
        summary = "멤버 강퇴",
        description = "모임장, 운영자 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @DeleteMapping("/{roomId}/members/{userId}")
    fun kickMember(@PathVariable roomId: Long, @PathVariable userId: Long): ApiResponse<Void> {
        val actorId = requireNotNull(rq.getActor().id)
        userRoomConnectionService.kickMember(actorId, roomId, userId)
        return ApiResponse.success(HttpStatus.OK, "멤버 강퇴 성공", null)
    }

    @Operation(summary = "모임 탈퇴", security = [SecurityRequirement(name = "BearerAuth")])
    @DeleteMapping("/{roomId}/members/me")
    fun leaveRoom(@PathVariable roomId: Long): ApiResponse<Void> {
        userRoomConnectionService.leaveRoom(roomId, requireNotNull(rq.getActor().id))
        return ApiResponse.success(HttpStatus.OK, "모임 탈퇴 성공", null)
    }
}
