package com.budzet.domain.invite.controller;

import com.budzet.domain.invite.dto.InviteJoinResponse;
import com.budzet.domain.invite.dto.InviteResponse;
import com.budzet.domain.invite.service.InviteService;
import com.budzet.global.api.ApiResponse;
import com.budzet.global.rq.Rq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "04. 초대", description = "초대 링크 생성, 목록 조회, 코드 참여 및 유효성 검증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class InviteController {

    private final InviteService inviteService;
    private final Rq rq;

    @Operation(
            summary = "초대 링크 생성",
            description = "모임장 권한 필요",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping("/{roomId}/invites")
    public ApiResponse<InviteResponse> createInvite(
            @PathVariable Long roomId
    ) {
        Long userId = rq.getActor().getId();

        InviteResponse response =
                inviteService.createInvite(roomId, userId);

        return ApiResponse.success(
                HttpStatus.CREATED,
                "초대 링크 생성 성공",
                response
        );
    }

    @Operation(
            summary = "초대 링크 목록 조회",
            description = "모임장 권한 필요",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @GetMapping("/{roomId}/invites")
    public ApiResponse<List<InviteResponse>> getInvites(
            @PathVariable Long roomId
    ) {
        List<InviteResponse> response =
                inviteService.getInvites(roomId, rq.getActor().getId());

        return ApiResponse.success(
                HttpStatus.OK,
                "초대 링크 조회 성공",
                response
        );
    }

    @Operation(
            summary = "초대 코드로 모임 참여",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping("/join/{code}")
    public ApiResponse<InviteJoinResponse> joinRoom(
            @PathVariable String code
    ) {
        Long userId = rq.getActor().getId();
        InviteJoinResponse response = inviteService.joinRoom(code, userId);

        return ApiResponse.success(
                HttpStatus.CREATED,
                "모임에 참여하였습니다.",
                response
        );
    }
}
