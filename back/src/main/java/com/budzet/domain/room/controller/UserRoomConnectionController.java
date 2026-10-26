package com.budzet.domain.room.controller;

import com.budzet.domain.room.dto.MemberResponse;
import com.budzet.domain.room.service.UserRoomConnectionService;
import com.budzet.global.api.ApiResponse;
import com.budzet.global.rq.Rq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "02 - 2. 멤버", description = "모임방 멤버 권한 조회/변경, 목록 조회, 강퇴 및 탈퇴 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class UserRoomConnectionController {

    private final UserRoomConnectionService userRoomConnectionService;
    private final Rq rq;

    @Operation(
            summary = "멤버 권한 조회",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @GetMapping("/{roomId}/members/{userId}/authority")
    public ApiResponse<String> getAuthority(
            @PathVariable Long roomId,
            @PathVariable Long userId
    ) {
        String authority = userRoomConnectionService
                .getConnection(roomId, rq.getActor().getId(), userId)
                .getAuthority()
                .name();

        return ApiResponse.success(
                HttpStatus.OK,
                "멤버 권한 조회 성공",
                authority
        );
    }

    @Operation(
            summary = "멤버 목록 조회",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @GetMapping("/{roomId}/members")
    public ApiResponse<List<MemberResponse>> getMembers(
            @PathVariable Long roomId
    ) {
        List<MemberResponse> members =
                userRoomConnectionService.getMembers(roomId, rq.getActor().getId());

        return ApiResponse.success(
                HttpStatus.OK,
                "멤버 목록 조회 성공",
                members
        );
    }

    @Operation(
            summary = "멤버 강퇴",
            description = "모임장, 운영자 권한 필요",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @DeleteMapping("/{roomId}/members/{userId}")
    public ApiResponse<Void> kickMember(
            @PathVariable Long roomId,
            @PathVariable Long userId
    ) {
        Long actorId = rq.getActor().getId();

        userRoomConnectionService.kickMember(actorId, roomId, userId);

        return ApiResponse.success(
                HttpStatus.OK,
                "멤버 강퇴 성공",
                null
        );
    }

    @Operation(
            summary = "모임 탈퇴",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @DeleteMapping("/{roomId}/members/me")
    public ApiResponse<Void> leaveRoom(
            @PathVariable Long roomId
    ) {
        //userID 임시 설정 -> 로그인 구현 후 userId 삽입
        userRoomConnectionService.leaveRoom(roomId, rq.getActor().getId());

        return ApiResponse.success(
                HttpStatus.OK,
                "모임 탈퇴 성공",
                null
        );
    }
}