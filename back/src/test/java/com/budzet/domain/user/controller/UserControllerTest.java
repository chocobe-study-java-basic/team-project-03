package com.budzet.domain.user.controller;

import com.budzet.domain.user.dto.*;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.service.UserService;
import com.budzet.global.api.ApiResponse;
import com.budzet.global.exception.BusinessException;
import com.budzet.global.exception.ErrorCode;
import com.budzet.global.rq.Rq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Test
    @DisplayName("회원 가입 성공")
    void join_success() {

        UserService service = mock(UserService.class);
        Rq rq = mock(Rq.class);

        UserController controller =
                new UserController(service, rq, 1000L, 2000L);

        String email = "user1@test.com";
        String password = "password123";
        String name = "user1";

        User user = new User(
                email,
                "encodedPassword",
                name
        );

        when(service.join(email, password, name))
                .thenReturn(user);

        UserJoinRequest request =
                new UserJoinRequest(email, password, name);

        ResponseEntity<ApiResponse<UserDto>> response =
                controller.join(request);

        ApiResponse<UserDto> body = response.getBody();

        assertNotNull(body);
        assertEquals(201, response.getStatusCode().value());
        assertEquals(201, body.getResultCode());
        assertEquals("회원가입이 완료되었습니다.", body.getMessage());
        assertEquals(email, body.getData().getEmail());
        assertEquals(name, body.getData().getName());

        verify(service).join(email, password, name);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {

        // given
        UserService service = mock(UserService.class);
        Rq rq = mock(Rq.class);

        UserController controller =
                new UserController(service, rq, 1000L, 2000L);

        String email = "user1@test.com";
        String password = "password123";
        String accessToken = "testAccessToken";
        String refreshToken = "testRefreshToken";

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn(email);
        when(user.getName()).thenReturn("user1");

        UserLoginResponse loginResponse =
                UserLoginResponse.from(
                        user,
                        accessToken,
                        refreshToken
                );

        when(service.login(email, password))
                .thenReturn(loginResponse);

        UserLoginRequest request =
                new UserLoginRequest(email, password);

        // when
        ResponseEntity<ApiResponse<UserLoginResponse>> response =
                controller.login(request);

        ApiResponse<UserLoginResponse> body = response.getBody();

        // then
        assertNotNull(body);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, body.getResultCode());
        assertEquals("로그인에 성공했습니다.", body.getMessage());

        assertEquals(accessToken, body.getData().getAccessToken());
        assertEquals(refreshToken, body.getData().getRefreshToken());
        assertEquals(email, body.getData().getUser().getEmail());
        assertEquals("user1", body.getData().getUser().getName());

        verify(service).login(email, password);

        verify(rq)
                .addCookie("accessToken", accessToken, Duration.ofMillis(1000L));

        verify(rq)
                .addCookie("refreshToken", refreshToken, Duration.ofMillis(2000L));
    }

    @Test
    @DisplayName("Refresh Token으로 토큰 재발급 성공")
    void refresh_success() {

        // given
        UserService service = mock(UserService.class);
        Rq rq = mock(Rq.class);

        UserController controller =
                new UserController(service, rq, 1000L, 2000L);

        String refreshToken = "oldRefreshToken";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        // Authorization 헤더가 없으므로 쿠키 사용
        when(rq.getHeader("Authorization", ""))
                .thenReturn("");

        when(rq.getCookieValue("refreshToken", ""))
                .thenReturn(refreshToken);

        TokenRefreshResponse tokenResponse =
                TokenRefreshResponse.from(
                        newAccessToken,
                        newRefreshToken
                );

        when(service.refresh(refreshToken))
                .thenReturn(tokenResponse);

        // when
        ResponseEntity<ApiResponse<TokenRefreshResponse>> response =
                controller.refresh();

        ApiResponse<TokenRefreshResponse> body =
                response.getBody();

        // then
        assertNotNull(body);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, body.getResultCode());
        assertEquals("토큰 재발급 성공", body.getMessage());

        assertEquals(
                newAccessToken,
                body.getData().getAccessToken()
        );

        assertEquals(
                newRefreshToken,
                body.getData().getRefreshToken()
        );

        verify(service)
                .refresh(refreshToken);

        verify(rq)
                .addCookie("accessToken", newAccessToken, Duration.ofMillis(1000L));

        verify(rq)
                .addCookie("refreshToken", newRefreshToken, Duration.ofMillis(2000L));
    }

    @Test
    @DisplayName("Refresh Token이 없으면 인증 실패")
    void refresh_noRefreshToken() {

        // given
        UserService service = mock(UserService.class);
        Rq rq = mock(Rq.class);

        UserController controller =
                new UserController(service, rq, 1000L, 2000L);

        when(rq.getHeader("Authorization", ""))
                .thenReturn("");

        when(rq.getCookieValue("refreshToken", ""))
                .thenReturn("");

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                controller::refresh
        );

        // then
        assertEquals(
                ErrorCode.UNAUTHORIZED,
                exception.getErrorCode()
        );

        verify(service, never())
                .refresh(anyString());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        // given
        UserService service = mock(UserService.class);
        Rq rq = mock(Rq.class);

        UserController controller =
                new UserController(service, rq, 1000L, 2000L);

        User user = mock(User.class);

        when(rq.getActor())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(1L);

        // when
        ResponseEntity<ApiResponse<Void>> response =
                controller.logout();

        // then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, response.getBody().getResultCode());
        assertEquals("로그아웃되었습니다.", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(rq).getActor();
        verify(service).logout(1L);

        verify(rq).deleteCookie("accessToken");
        verify(rq).deleteCookie("refreshToken");
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void me_success() {

        // given
        UserService service = mock(UserService.class);
        Rq rq = mock(Rq.class);

        UserController controller =
                new UserController(service, rq, 1000L, 2000L);

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("user1@test.com");
        when(user.getName()).thenReturn("user1");

        when(rq.getActor())
                .thenReturn(user);

        // when
        ResponseEntity<ApiResponse<UserDto>> response =
                controller.me();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ApiResponse<UserDto> body = response.getBody();

        assertNotNull(body);
        assertEquals(200, body.getResultCode());
        assertEquals("내 정보 조회에 성공했습니다.", body.getMessage());

        assertEquals(1L, body.getData().getId());
        assertEquals("user1@test.com", body.getData().getEmail());
        assertEquals("user1", body.getData().getName());

        verify(rq).getActor();
    }
}