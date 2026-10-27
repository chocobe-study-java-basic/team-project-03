package com.budzet.domain.room.controller

import com.budzet.domain.room.dto.*
import com.budzet.domain.room.service.RoomService
import com.budzet.global.api.ApiResponse
import com.budzet.global.rq.Rq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "02 - 1. 모임방", description = "모임방 생성, 목록/상세 조회, 정보 수정, 삭제 및 권한 위임/지정 API")
@RestController
@RequestMapping("/rooms")
class RoomController(
    private val roomService: RoomService,
    private val rq: Rq,
) {
    @Operation(
        summary = "모임 생성",
        description = "새로운 모임을 생성, 생성한 사용자는 모임장이 된다.",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @PostMapping
    fun createRoom(@RequestBody @Valid request: RoomCreateRequest): ResponseEntity<ApiResponse<RoomCreateResponse>> {
        val userId = requireNotNull(rq.getActor().id)
        val response = roomService.createRoom(userId, request)
        return ApiResponse.response(HttpStatus.CREATED, "모임이 생성되었습니다.", response)
    }

    @Operation(
        summary = "모임 목록 조회",
        description = "참여중인 모임 전체 조회",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @GetMapping
    fun getRooms(): ResponseEntity<ApiResponse<RoomListResponse>> {
        val userId = requireNotNull(rq.getActor().id)
        val response = roomService.getRooms(userId)
        return ApiResponse.response(HttpStatus.OK, "모임 목록을 조회했습니다.", response)
    }

    @Operation(summary = "모임 상세 조회", security = [SecurityRequirement(name = "BearerAuth")])
    @GetMapping("/{roomId}")
    fun getRoom(@PathVariable roomId: Long): ResponseEntity<ApiResponse<RoomDetailResponse>> {
        val userId = requireNotNull(rq.getActor().id)
        val response = roomService.getRoom(userId, roomId)
        return ApiResponse.response(HttpStatus.OK, "모임을 조회했습니다.", response)
    }

    @Operation(
        summary = "모임 정보 수정",
        description = "모임장 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @PatchMapping("/{roomId}")
    fun updateRoom(
        @PathVariable roomId: Long,
        @RequestBody @Valid request: RoomUpdateRequest,
    ): ResponseEntity<ApiResponse<RoomDetailResponse>> {
        val userId = requireNotNull(rq.getActor().id)
        val response = roomService.updateRoom(userId, roomId, request)
        return ApiResponse.response(HttpStatus.OK, "모임이 수정되었습니다.", response)
    }

    @Operation(
        summary = "모임 삭제",
        description = "모임장 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @DeleteMapping("/{roomId}")
    fun deleteRoom(@PathVariable roomId: Long): ResponseEntity<ApiResponse<Void>> {
        val userId = requireNotNull(rq.getActor().id)
        roomService.deleteRoom(userId, roomId)
        return ApiResponse.response(HttpStatus.OK, "모임을 삭제했습니다.", null)
    }

    @Operation(
        summary = "운영자 지정/해제",
        description = "모임장 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @PatchMapping("/{roomId}/members/{userId}/authority")
    fun changeAuthority(
        @PathVariable roomId: Long,
        @PathVariable userId: Long,
        @RequestBody @Valid request: AuthorityChangeRequest,
    ): ResponseEntity<ApiResponse<Void>> {
        val actorId = requireNotNull(rq.getActor().id)
        roomService.changeAuthority(actorId, roomId, userId, request)
        return ApiResponse.response(HttpStatus.OK, "멤버 권한이 변경되었습니다.", null)
    }

    @Operation(
        summary = "모임장 권한 위임",
        description = "모임장 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @PatchMapping("/{roomId}/members/{userId}/owner")
    fun delegateOwner(
        @PathVariable roomId: Long,
        @PathVariable userId: Long,
    ): ResponseEntity<ApiResponse<Void>> {
        val actorId = requireNotNull(rq.getActor().id)
        roomService.delegateOwner(actorId, roomId, userId)
        return ApiResponse.response(HttpStatus.OK, "모임장 권한이 위임되었습니다.", null)
    }
}
