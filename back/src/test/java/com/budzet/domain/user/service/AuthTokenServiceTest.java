package com.budzet.domain.user.service;

import com.budzet.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthTokenServiceTest {

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService(
                "test-access-secret-for-budzet-0123456789",
                3_600_000L,
                "test-refresh-secret-for-budzet-0123456789",
                604_800_000L
        );
    }

    @Test
    @DisplayName("Access Token 생성 및 Payload 조회 성공")
    void genAccessToken_success() {

        User user = new User(
                "user1@test.com",
                "encodedPassword",
                "user1"
        );

        ReflectionTestUtils.setField(user, "id", 1L);

        String accessToken =
                authTokenService.genAccessToken(user);

        assertNotNull(accessToken);

        Map<String, Object> payload =
                authTokenService.accessPayloadOrNull(accessToken);

        assertNotNull(payload);
        assertEquals(1L, payload.get("id"));
    }

    @Test
    @DisplayName("유효하지 않은 Access Token은 Payload 조회 실패")
    void invalidAccessToken() {

        String invalidToken = "invalid.jwt.token";

        Map<String, Object> payload =
                authTokenService.accessPayloadOrNull(invalidToken);

        assertNull(payload);
    }
}
