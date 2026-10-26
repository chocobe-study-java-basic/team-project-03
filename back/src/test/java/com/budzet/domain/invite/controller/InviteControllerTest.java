package com.budzet.domain.invite.controller;

import com.budzet.domain.invite.dto.InviteJoinResponse;
import com.budzet.domain.invite.dto.InviteResponse;
import com.budzet.domain.invite.service.InviteService;
import com.budzet.domain.user.entity.User;
import com.budzet.global.api.ApiResponse;
import com.budzet.global.rq.Rq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InviteControllerTest {

    private InviteService inviteService;
    private Rq rq;
    private InviteController controller;

    @BeforeEach
    void setUp() {
        inviteService = mock(InviteService.class);
        rq = mock(Rq.class);

        controller = new InviteController(
                inviteService,
                rq
        );
    }

    @Test
    void CREATE_INVITE_LINK() {
        // given
        Long roomId = 1L;
        Long userId = 2L;

        User user = mock(User.class);

        InviteResponse inviteResponse =
                new InviteResponse(
                        "ABC1234567",
                        LocalDateTime.now().plusHours(1)
                );

        when(rq.getActor())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(userId);

        when(inviteService.createInvite(roomId, userId))
                .thenReturn(inviteResponse);

        // when
        var response =
                controller.createInvite(roomId);

        // then
        assertThat(response.resultCode())
                .isEqualTo(201);

        assertThat(response.message())
                .isEqualTo("초대 링크 생성 성공");

        assertThat(response.data())
                .isEqualTo(inviteResponse);

        verify(rq)
                .getActor();

        verify(user)
                .getId();

        verify(inviteService)
                .createInvite(roomId, userId);
    }

    @Test
    void JOIN_ROOM() {
        String code = "ABC1234567";
        Long userId = 2L;
        User user = mock(User.class);
        InviteJoinResponse joinResponse = new InviteJoinResponse(
                userId, 1L, "MEMBER", true, LocalDateTime.now()
        );

        when(rq.getActor()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(inviteService.joinRoom(code, userId)).thenReturn(joinResponse);

        ApiResponse<InviteJoinResponse> response = controller.joinRoom(code);

        assertThat(response.resultCode()).isEqualTo(201);
        assertThat(response.message()).isEqualTo("모임에 참여하였습니다.");
        assertThat(response.data()).isEqualTo(joinResponse);
        verify(inviteService).joinRoom(code, userId);
    }

    @Test
    void getInvites() {
        Long roomId = 1L;
        User actor = mock(User.class);
        when(rq.getActor()).thenReturn(actor);
        when(actor.getId()).thenReturn(2L);

        List<InviteResponse> inviteResponses = List.of(
                new InviteResponse(
                        "ABC1234567",
                        LocalDateTime.now().plusHours(1)
                ),
                new InviteResponse(
                        "DEF1234567",
                        LocalDateTime.now().plusHours(1)
                )
        );

        when(inviteService.getInvites(roomId, 2L))
                .thenReturn(inviteResponses);

        ApiResponse<List<InviteResponse>> response =
                controller.getInvites(roomId);

        assertThat(response.resultCode()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("초대 링크 조회 성공");
        assertThat(response.data()).isEqualTo(inviteResponses);

        verify(inviteService).getInvites(roomId, 2L);
    }
}
