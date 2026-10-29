package com.budzet.global.security;

import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.service.UserService;
import com.budzet.global.rq.Rq;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationFilterTest {

    @Mock
    private Rq rq;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private MockHttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CustomAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();

        when(request.getRequestURI())
                .thenReturn("/rooms");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더의 Access Token으로 인증에 성공한다")
    void authentication_header_success() throws Exception {

        // given
        String accessToken = "validAccessToken";

        User user = new User(
                "test@test.com",
                "encodedPassword",
                "홍길동"
        );

        when(rq.getHeader("Authorization", ""))
                .thenReturn("Bearer " + accessToken);

        when(userService.accessPayloadOrNull(accessToken))
                .thenReturn(Map.of(
                        "id", 1L,
                        "name", "홍길동"
                ));

        when(userService.findByIdOrNull(1L))
                .thenReturn(user);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertInstanceOf(
                SecurityUser.class,
                authentication.getPrincipal()
        );

        SecurityUser securityUser =
                (SecurityUser) authentication.getPrincipal();

        assertSame(user, securityUser.getUser());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("쿠키의 Access Token으로 인증에 성공한다")
    void authentication_cookie_success() throws Exception {

        // given
        String accessToken = "validAccessToken";

        User user = new User(
                "test@test.com",
                "encodedPassword",
                "홍길동"
        );

        when(rq.getHeader("Authorization", ""))
                .thenReturn("");

        when(rq.getCookieValue("accessToken", ""))
                .thenReturn(accessToken);

        when(userService.accessPayloadOrNull(accessToken))
                .thenReturn(Map.of(
                        "id", 1L,
                        "name", "홍길동"
                ));

        when(userService.findByIdOrNull(1L))
                .thenReturn(user);
        // when
        filter.doFilter(request, response, filterChain);

        // then
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);

        SecurityUser securityUser =
                (SecurityUser) authentication.getPrincipal();

        assertSame(user, securityUser.getUser());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Access Token이 없으면 인증 정보 없이 다음 필터로 넘어간다")
    void authentication_noAccessToken() throws Exception {

        // given
        when(rq.getHeader("Authorization", ""))
                .thenReturn("");

        when(rq.getCookieValue("accessToken", ""))
                .thenReturn("");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNull(authentication);

        verify(filterChain).doFilter(request, response);

        verify(userService, never())
                .accessPayloadOrNull(anyString());
    }

    @Test
    @DisplayName("유효하지 않은 Access Token이면 401 응답을 반환한다")
    void authentication_invalidAccessToken() throws Exception {

        // given
        String accessToken = "invalidAccessToken";

        when(rq.getHeader("Authorization", ""))
                .thenReturn("Bearer " + accessToken);

        when(userService.accessPayloadOrNull(accessToken))
                .thenReturn(null);

        // when
        filter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertEquals(401, response.getStatus());

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(filterChain, never())
                .doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 Authorization 헤더면 401 응답을 반환한다")
    void authentication_invalidAuthorizationHeader() throws Exception {

        // given
        when(rq.getHeader("Authorization", ""))
                .thenReturn("invalidAccessToken");

        // when
        filter.doFilter(
                request,
                response,
                filterChain
        );

        // then
        assertEquals(401, response.getStatus());

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(filterChain, never())
                .doFilter(request, response);
    }

    @Test
    @DisplayName("로그인 요청은 인증 필터를 적용하지 않는다")
    void login_shouldNotFilter() {

        // given
        when(request.getRequestURI())
                .thenReturn("/users/login");

        // when
        boolean result = filter.shouldNotFilter(request);

        // then
        assertTrue(result);
    }
}