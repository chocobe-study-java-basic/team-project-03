package com.budzet.domain.room.controller;

import com.budzet.domain.room.dto.MemberResponse;
import com.budzet.domain.room.entity.Authority;
import com.budzet.domain.room.entity.UserRoomConnection;
import com.budzet.domain.room.service.UserRoomConnectionService;
import com.budzet.domain.user.entity.User;
import com.budzet.global.rq.Rq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserRoomConnectionControllerTest {

    private UserRoomConnectionService service;
    private Rq rq;
    private UserRoomConnectionController controller;

    @BeforeEach
    void setUp() {
        service = mock(UserRoomConnectionService.class);
        rq = mock(Rq.class);

        controller = new UserRoomConnectionController(service, rq);
    }

    @Test
    void 멤버_권한_조회() {
        User actor = mock(User.class);
        when(rq.getActor()).thenReturn(actor);
        when(actor.getId()).thenReturn(3L);
        // given
        Long roomId = 1L;
        Long userId = 2L;

        UserRoomConnection connection =
                mock(UserRoomConnection.class);

        when(service.getConnection(roomId, 3L, userId))
                .thenReturn(connection);

        when(connection.getAuthority())
                .thenReturn(Authority.MEMBER);

        // when
        var response =
                controller.getAuthority(roomId, userId);

        // then
        assertThat(response.getResultCode())
                .isEqualTo(200);

        assertThat(response.getMessage())
                .isEqualTo("멤버 권한 조회 성공");

        assertThat(response.getData())
                .isEqualTo("MEMBER");

        verify(service)
                .getConnection(roomId, 3L, userId);
    }

    @Test
    void 멤버_목록_조회() {
        User actor = mock(User.class);
        when(rq.getActor()).thenReturn(actor);
        when(actor.getId()).thenReturn(3L);
        // given
        Long roomId = 1L;

        MemberResponse member1 =
                mock(MemberResponse.class);

        MemberResponse member2 =
                mock(MemberResponse.class);

        when(service.getMembers(roomId, 3L))
                .thenReturn(List.of(member1, member2));

        // when
        var response =
                controller.getMembers(roomId);

        // then
        assertThat(response.getResultCode())
                .isEqualTo(200);

        assertThat(response.getMessage())
                .isEqualTo("멤버 목록 조회 성공");

        assertThat(response.getData())
                .hasSize(2);

        verify(service)
                .getMembers(roomId, 3L);
    }

    @Test
    void 멤버_강퇴() {
        // given
        Long roomId = 1L;
        Long targetUserId = 2L;
        Long actorId = 1L;

        User user = mock(User.class);

        when(rq.getActor())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(actorId);

        // when
        var response =
                controller.kickMember(roomId, targetUserId);

        // then
        assertThat(response.getResultCode())
                .isEqualTo(200);

        assertThat(response.getMessage())
                .isEqualTo("멤버 강퇴 성공");

        assertThat(response.getData())
                .isNull();

        verify(rq)
                .getActor();

        verify(user)
                .getId();

        verify(service)
                .kickMember(
                        actorId,
                        roomId,
                        targetUserId
                );
    }

    @Test
    void 모임_탈퇴() {
        // given
        Long roomId = 1L;
        Long userId = 2L;

        User user = mock(User.class);

        when(rq.getActor())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(userId);

        // when
        var response =
                controller.leaveRoom(roomId);

        // then
        assertThat(response.getResultCode())
                .isEqualTo(200);

        assertThat(response.getMessage())
                .isEqualTo("모임 탈퇴 성공");

        assertThat(response.getData())
                .isNull();

        verify(rq)
                .getActor();

        verify(user)
                .getId();

        verify(service)
                .leaveRoom(roomId, userId);
    }


}