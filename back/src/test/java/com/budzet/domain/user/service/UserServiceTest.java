package com.budzet.domain.user.service;

import com.budzet.domain.user.dto.TokenRefreshResponse;
import com.budzet.domain.user.dto.UserLoginResponse;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.repository.UserRepository;
import com.budzet.global.exception.BusinessException;
import com.budzet.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원 가입 성공")
    void join_success() {

        String email = "user1@test.com";
        String password = "password123";
        String encodedPassword = "encodedPassword";
        String name = "user1";

        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        when(passwordEncoder.encode(password))
                .thenReturn(encodedPassword);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result =
                userService.join(email, password, name);

        assertEquals(email, result.getEmail());
        assertEquals(encodedPassword, result.getPassword());
        assertEquals(name, result.getName());
    }

    @Test
    @DisplayName("회원 가입, 이미 존재하는 회원")
    void join_userAlreadyExists() {

        String email = "user1@test.com";
        String password = "password123";
        String name = "user1";

        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.join(email, password, name)
        );

        assertEquals(
                ErrorCode.USER_CONFLICT,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("로그인 성공 시 Access Token과 Refresh Token을 발급하고 Refresh Token을 저장한다")
    void login_success() {

        // given
        String email = "user1@test.com";
        String password = "password123";
        String encodedPassword = "encodedPassword";

        String accessToken = "accessToken";
        String refreshToken = "refreshToken";

        User user = new User(
                email,
                encodedPassword,
                "user1"
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(password, encodedPassword))
                .thenReturn(true);

        when(authTokenService.genAccessToken(user))
                .thenReturn(accessToken);

        when(authTokenService.genRefreshToken(user))
                .thenReturn(refreshToken);

        // when
        UserLoginResponse response =
                userService.login(email, password);

        // then
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());

        // DB에 저장될 User의 Refresh Token도 변경되었는지 확인
        assertEquals(refreshToken, user.getRefreshToken());

        verify(passwordEncoder)
                .matches(password, encodedPassword);

        verify(authTokenService)
                .genAccessToken(user);

        verify(authTokenService)
                .genRefreshToken(user);
    }

    @Test
    @DisplayName("Refresh Token 재발급 성공")
    void refresh_success() {

        // given
        Long userId = 1L;

        String oldRefreshToken = "oldRefreshToken";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        User user = new User(
                "user1@test.com",
                "encodedPassword",
                "user1"
        );

        user.updateRefreshToken(oldRefreshToken);

        when(authTokenService.refreshPayloadOrNull(oldRefreshToken))
                .thenReturn(Map.of(
                        "id", userId,
                        "name", "user1"
                ));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(authTokenService.genAccessToken(user))
                .thenReturn(newAccessToken);

        when(authTokenService.genRefreshToken(user))
                .thenReturn(newRefreshToken);

        // when
        TokenRefreshResponse response =
                userService.refresh(oldRefreshToken);

        // then
        assertEquals(newAccessToken, response.accessToken());
        assertEquals(newRefreshToken, response.refreshToken());

        // 기존 RT가 새 RT로 교체됐는지 확인
        assertEquals(newRefreshToken, user.getRefreshToken());

        verify(authTokenService)
                .refreshPayloadOrNull(oldRefreshToken);

        verify(userRepository)
                .findById(userId);

        verify(authTokenService)
                .genAccessToken(user);

        verify(authTokenService)
                .genRefreshToken(user);
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token이면 재발급에 실패한다")
    void refresh_invalidRefreshToken() {

        // given
        String refreshToken = "invalidRefreshToken";

        when(authTokenService.refreshPayloadOrNull(refreshToken))
                .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.refresh(refreshToken)
        );

        // then
        assertEquals(
                ErrorCode.INVALID_REFRESH_TOKEN,
                exception.getErrorCode()
        );

        verify(userRepository, never())
                .findById(any());
    }

    @Test
    @DisplayName("DB에 저장된 Refresh Token과 일치하지 않으면 재발급에 실패한다")
    void refresh_refreshTokenMismatch() {

        // given
        Long userId = 1L;

        String requestRefreshToken = "requestRefreshToken";
        String savedRefreshToken = "savedRefreshToken";

        User user = new User(
                "user1@test.com",
                "encodedPassword",
                "user1"
        );

        user.updateRefreshToken(savedRefreshToken);

        when(authTokenService.refreshPayloadOrNull(requestRefreshToken))
                .thenReturn(Map.of(
                        "id", userId,
                        "name", "user1"
                ));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.refresh(requestRefreshToken)
        );

        // then
        assertEquals(
                ErrorCode.INVALID_REFRESH_TOKEN,
                exception.getErrorCode()
        );

        verify(authTokenService, never())
                .genAccessToken(any());

        verify(authTokenService, never())
                .genRefreshToken(any());
    }

    @Test
    @DisplayName("비밀번호 일치")
    void checkPassword_success() {

        String inputPassword = "password123";
        String encodedPassword = "encodedPassword";

        when(passwordEncoder.matches(inputPassword, encodedPassword))
                .thenReturn(true);

        userService.checkPassword(
                inputPassword,
                encodedPassword
        );

        verify(passwordEncoder)
                .matches(inputPassword, encodedPassword);
    }

    @Test
    @DisplayName("비밀번호 불일치")
    void checkPassword_mismatch() {

        String inputPassword = "wrongPassword";
        String encodedPassword = "encodedPassword";

        when(passwordEncoder.matches(inputPassword, encodedPassword))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.checkPassword(
                        inputPassword,
                        encodedPassword
                )
        );

        assertEquals(
                ErrorCode.PASSWORD_MISMATCH,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("로그아웃 성공 시 Refresh Token을 삭제한다")
    void logout_success() {
        // given
        Long userId = 1L;

        User user = new User(
                "user1@test.com",
                "encodedPassword",
                "user1"
        );

        user.updateRefreshToken("refreshToken");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // when
        userService.logout(userId);

        // then
        assertNull(user.getRefreshToken());

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("로그아웃 시 사용자를 찾을 수 없으면 예외가 발생한다")
    void logout_userNotFound() {
        // given
        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.logout(userId)
        );

        // then
        assertEquals(
                ErrorCode.USER_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("비밀번호 변경 대상 회원이 없으면 암호화를 수행하지 않는다")
    void updatePassword_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updatePassword(1L, "password123", "newPassword123"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(passwordEncoder, authTokenService);
    }
}
