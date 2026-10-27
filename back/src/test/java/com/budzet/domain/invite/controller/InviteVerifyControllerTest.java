package com.budzet.domain.invite.controller;

import com.budzet.domain.invite.dto.InviteResponse;
import com.budzet.domain.invite.service.InviteService;
import com.budzet.global.api.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InviteVerifyControllerTest {

    private InviteService inviteService;
    private InviteVerifyController controller;

    @BeforeEach
    void setUp() {
        inviteService = mock(InviteService.class);
        controller = new InviteVerifyController(inviteService);
    }

    @Test
    void verifyInvite() {
        String token = "ABC1234567";

        InviteResponse inviteResponse = new InviteResponse(
                token,
                LocalDateTime.now().plusHours(1)
        );

        when(inviteService.verifyInvite(token))
                .thenReturn(inviteResponse);

        ApiResponse<InviteResponse> response =
                controller.verifyInvite(token);

        assertThat(response.getResultCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("초대 링크 검증 성공");
        assertThat(response.getData()).isEqualTo(inviteResponse);

        verify(inviteService).verifyInvite(token);
    }
}