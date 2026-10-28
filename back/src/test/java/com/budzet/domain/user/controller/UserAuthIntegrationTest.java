package com.budzet.domain.user.controller;

import com.budzet.domain.user.dto.UserJoinRequest;
import com.budzet.domain.user.dto.UserLoginRequest;
import com.budzet.domain.user.dto.UserProfileUpdateRequest;
import jakarta.servlet.http.Cookie;
import jakarta.persistence.EntityManager;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;


import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Value("${custom.jwt.access-expire-millis}")
    private long accessExpireMillis;

    @Value("${custom.jwt.refresh-expire-millis}")
    private long refreshExpireMillis;

    @Value("${custom.jwt.access-secret}")
    private String accessSecret;

    @Value("${custom.jwt.refresh-secret}")
    private String refreshSecret;

    private final String email = UUID.randomUUID() + "@test.com";

    @AfterEach
    void tearDown() {
        // 다른 테스트의 데이터는 건드리지 않고 이 테스트에서 만든 회원만 정리한다.
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
    }

    @Test
    @Transactional
    @DisplayName("회원 엔티티의 프록시·지연 로딩·감사와 변경 감지를 유지한다")
    void userPersistenceSupportsProxyAuditingAndDirtyChecking() {
        User user = new User(email, "encodedPassword", "기존 이름");
        assertEquals(0L, user.getId());
        assertNull(user.getCreatedAt());
        assertNull(user.getModifiedAt());
        assertNull(user.getRefreshToken());
        assertTrue(user.getRoomConnections().isEmpty());

        entityManager.persist(user);
        entityManager.flush();
        Long id = user.getId();
        assertTrue(id > 0L);
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getModifiedAt());
        entityManager.clear();

        User reference = entityManager.getReference(User.class, id);
        assertFalse(Hibernate.isInitialized(reference));
        assertEquals(email, reference.getEmail());
        assertTrue(Hibernate.isInitialized(reference));
        assertFalse(Hibernate.isInitialized(reference.getRoomConnections()));
        assertTrue(reference.getRoomConnections().isEmpty());
        assertTrue(Hibernate.isInitialized(reference.getRoomConnections()));

        reference.updateName("변경된 이름");
        reference.updateRefreshToken("saved-refresh-token");
        entityManager.flush();
        entityManager.clear();

        User reloaded = entityManager.find(User.class, id);
        assertEquals("변경된 이름", reloaded.getName());
        assertEquals("saved-refresh-token", reloaded.getRefreshToken());
        assertNotNull(reloaded.getModifiedAt());
        reloaded.clearRefreshToken();
        entityManager.flush();
        entityManager.clear();
        assertNull(entityManager.find(User.class, id).getRefreshToken());
    }

    @Test
    @DisplayName("회원가입 요청 시 회원을 DB에 저장하고 비밀번호를 암호화한다")
    void join_success() throws Exception {
        // given
        String password = "password123";
        String name = "테스트 회원";
        UserJoinRequest request = new UserJoinRequest(email, password, name);

        // when: 인증 없이 실제 Security 필터와 회원가입 API를 통과한다.
        MvcResult result = mockMvc.perform(post("/users/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(201))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        // then: 요청의 트랜잭션이 끝난 뒤 DB에서 다시 조회한다.
        User savedUser = userRepository.findByEmail(email).orElseThrow();

        assertNotNull(savedUser.getId());
        assertEquals(email, savedUser.getEmail());
        assertEquals(name, savedUser.getName());
        assertNotEquals(password, savedUser.getPassword());
        assertTrue(passwordEncoder.matches(password, savedUser.getPassword()));
        assertNull(savedUser.getRefreshToken());
        jsonPath("$.data.id").value(savedUser.getId()).match(result);
    }

    @Test
    @DisplayName("로그인 시 인증 쿠키를 발급하고 Refresh Token을 DB에 저장한다")
    void login_success() throws Exception {
        // given: 다른 테스트에 의존하지 않고 실제 암호화된 회원을 준비한다.
        String password = "password123";
        String name = "테스트 회원";
        User user = userRepository.save(
                new User(email, passwordEncoder.encode(password), name)
        );
        UserLoginRequest request = new UserLoginRequest(email, password);

        // when
        MvcResult result = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(200))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.user.id").value(user.getId()))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.name").value(name))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.refreshToken").doesNotExist())
                .andReturn();

        // then: 실제 Set-Cookie 응답의 값과 보안 속성을 확인한다.
        Cookie accessCookie = result.getResponse().getCookie("accessToken");
        Cookie refreshCookie = result.getResponse().getCookie("refreshToken");
        assertAuthenticationCookie(accessCookie, accessExpireMillis);
        assertAuthenticationCookie(refreshCookie, refreshExpireMillis);
        assertNotEquals(accessCookie.getValue(), refreshCookie.getValue());
        jsonPath("$.data.accessToken").value(accessCookie.getValue()).match(result);
        jsonPath("$.data.refreshToken").value(refreshCookie.getValue()).match(result);

        // 로그인 요청의 트랜잭션이 끝난 뒤 DB를 다시 조회한다.
        User savedUser = userRepository.findByEmail(email).orElseThrow();
        assertEquals(refreshCookie.getValue(), savedUser.getRefreshToken());
        assertEquals(user.getPassword(), savedUser.getPassword());
    }

    // 내 정보
    @Test
    @DisplayName("로그인으로 발급받은 Access Token 쿠키로 내 정보를 조회한다")
    void me_withLoginCookie_success() throws Exception {
        // given: 실제 로그인 API에서 인증 쿠키를 발급받는다.
        String password = "password123";
        String name = "테스트 회원";
        User user = userRepository.save(
                new User(email, passwordEncoder.encode(password), name)
        );
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserLoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie accessCookie = loginResult.getResponse().getCookie("accessToken");
        assertNotNull(accessCookie);

        // when & then: 인증 상태를 직접 주입하지 않고 JWT 필터를 통해 회원을 식별한다.
        mockMvc.perform(get("/users/me").cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(200))
                .andExpect(jsonPath("$.message").value("내 정보 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

        // 인증된 요청 직후에도 토큰 없는 요청에는 이전 인증 상태가 남지 않아야 한다.
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("인증된 회원은 정보를 수정할 수 있고 변경 내용이 DB에 반영된다")
    void updateProfile_success() throws Exception {
        // given
        User user = createMember();
        Cookie accessCookie = login().getResponse().getCookie("accessToken");
        assertNotNull(accessCookie);

        String newName = "변경된 이름";

        // when & then
        mockMvc.perform(patch("/users/me")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserProfileUpdateRequest(newName)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value(200))
                .andExpect(jsonPath("$.message").value("내 정보가 수정되었습니다."))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.name").value(newName));

        // DB에 실제로 변경되었는지 확인
        User updatedUser = storedMember();

        assertEquals(newName, updatedUser.getName());
    }

    @ParameterizedTest
    @MethodSource("invalidProfileNames")
    @DisplayName("잘못된 이름으로 수정 요청 시 400을 반환하고 기존 이름을 유지한다")
    void updateProfile_invalidName(String name, boolean omitted) throws Exception {
        // given
        createMember();
        Cookie accessCookie = login().getResponse().getCookie("accessToken");
        assertNotNull(accessCookie);

        String originalName = storedMember().getName();

        Map<String, Object> body = new HashMap<>();
        if (!omitted) {
            body.put("name", name);
        }

        // when & then
        mockMvc.perform(patch("/users/me")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value(400))
                .andExpect(jsonPath("$.data[*].field",
                        org.hamcrest.Matchers.hasItem("name")))
                .andExpect(header().doesNotExist("Set-Cookie"));

        // 검증 실패 후에도 DB의 이름이 유지되는지 확인
        assertEquals(originalName, storedMember().getName());
    }

    static Stream<Arguments> invalidProfileNames() {
        return Stream.of(
                Arguments.of("", false),
                Arguments.of("   ", false),
                Arguments.of(null, false),
                Arguments.of(null, true)
        );
    }

    // 토큰 재발급
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("쿠키 또는 Bearer 헤더의 Refresh Token으로 재발급하고 새 Access Token을 사용한다")
    void refresh_success(boolean useHeader) throws Exception {
        User user = createMember();
        // 실제 로그인에서 발급받은 Refresh Token으로 재발급 흐름을 검증한다.
        Cookie loginRefresh = login().getResponse().getCookie("refreshToken");
        assertNotNull(loginRefresh);
        String oldToken = loginRefresh.getValue();

        var request = post("/users/refresh")
                .cookie(new Cookie("accessToken", signedToken(accessSecret, user.getId(), true)));

        if (useHeader) request.header("Authorization", "Bearer " + oldToken);
        else request.cookie(new Cookie("refreshToken", oldToken));

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value(200))
                .andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
                .andReturn();

        Cookie access = result.getResponse().getCookie("accessToken");
        Cookie refresh = result.getResponse().getCookie("refreshToken");
        assertAuthenticationCookie(access, accessExpireMillis);
        assertAuthenticationCookie(refresh, refreshExpireMillis);
        jsonPath("$.data.accessToken").value(access.getValue()).match(result);
        jsonPath("$.data.refreshToken").value(refresh.getValue()).match(result);
        assertEquals(refresh.getValue(), storedMember().getRefreshToken());
        mockMvc.perform(get("/users/me").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId()));
    }

    @Test
    @DisplayName("이전에 발급된 Refresh Token을 DB에서 교체하고 이전 토큰의 재사용을 거부한다")
    void refresh_replacesStoredToken() throws Exception {
        User user = createMember();
        // 시간 조건을 구분해 저장된 토큰 교체를 검증한다.
        String oldToken = signedToken(refreshSecret, user.getId(), false);
        user.updateRefreshToken(oldToken);
        userRepository.save(user);

        MvcResult result = mockMvc.perform(post("/users/refresh")
                        .cookie(new Cookie("refreshToken", oldToken)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = result.getResponse().getCookie("refreshToken");
        assertNotNull(refresh);
        assertFalse(refresh.getValue().isBlank());
        assertNotEquals(oldToken, refresh.getValue());
        assertEquals(refresh.getValue(), storedMember().getRefreshToken());

        assertRefreshRejected(oldToken, 401, "유효한 Refresh Token이 아닙니다.");
        assertEquals(refresh.getValue(), storedMember().getRefreshToken());
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "tampered", "malformed", "access", "mismatch", "cleared"})
    @DisplayName("잘못된 Refresh Token의 재발급을 거부하고 DB 상태를 유지한다")
    void refresh_invalidToken(String kind) throws Exception {
        User user = createMember();
        MvcResult loggedIn = login();
        String savedToken = storedMember().getRefreshToken();
        String token = switch (kind) {
            case "expired" -> signedToken(refreshSecret, user.getId(), true);
            case "tampered" -> tamper(savedToken);
            case "malformed" -> "invalid.jwt.token";
            case "access" -> loggedIn.getResponse().getCookie("accessToken").getValue();
            case "mismatch" -> signedToken(refreshSecret, user.getId(), false);
            default -> savedToken;
        };
        if (kind.equals("cleared")) {
            user = storedMember();
            user.clearRefreshToken();
            userRepository.save(user);
            savedToken = null;
        }
        assertRefreshRejected(token, 401, "유효한 Refresh Token이 아닙니다.");
        assertEquals(savedToken, storedMember().getRefreshToken());
    }

    @Test
    @DisplayName("토큰의 회원이 존재하지 않으면 재발급에 실패한다")
    void refresh_memberNotFound() throws Exception {
        assertRefreshRejected(signedToken(refreshSecret, Long.MAX_VALUE, false),
                404, "존재하지 않는 회원입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "blank", "invalidHeader", "emptyBearer"})
    @DisplayName("Refresh Token 누락 또는 잘못된 헤더를 거부한다")
    void refresh_missingOrInvalidHeader(String kind) throws Exception {
        createMember();
        login();
        String savedToken = storedMember().getRefreshToken();

        var request = post("/users/refresh");

        if (kind.equals("blank")) request.cookie(new Cookie("refreshToken", " "));
        if (kind.equals("invalidHeader")) request.header("Authorization", "Basic invalid");
        if (kind.equals("emptyBearer")) request.header("Authorization", "Bearer ");

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value(401))
                .andExpect(jsonPath("$.message").value(kind.equals("invalidHeader")
                        ? "Authorization 헤더 형식이 올바르지 않습니다." : "인증이 필요합니다."))
                .andExpect(header().doesNotExist("Set-Cookie"));

        assertEquals(savedToken, storedMember().getRefreshToken());
    }

    // 로그아웃
    @Test
    @DisplayName("로그아웃은 DB의 Refresh Token과 쿠키를 삭제하고 이후 재발급을 차단한다")
    void logout_success() throws Exception {
        createMember();
        MvcResult loggedIn = login();
        Cookie access = loggedIn.getResponse().getCookie("accessToken");
        String oldRefresh = loggedIn.getResponse().getCookie("refreshToken").getValue();

        MvcResult result = mockMvc.perform(delete("/users/logout").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value(200))
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist()).andReturn();

        for (String cookieName : new String[]{"accessToken", "refreshToken"}) {
            Cookie cookie = result.getResponse().getCookie(cookieName);
            assertNotNull(cookie);
            assertEquals("", cookie.getValue());
            assertEquals(0, cookie.getMaxAge());
            assertEquals("/", cookie.getPath());
            assertTrue(cookie.isHttpOnly());
            assertTrue(cookie.getSecure());
            assertEquals("Lax", cookie.getAttribute("SameSite"));
        }

        assertNull(storedMember().getRefreshToken());
        assertRefreshRejected(oldRefresh, 401, "유효한 Refresh Token이 아닙니다.");
        assertNull(storedMember().getRefreshToken());
        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비인증 로그아웃은 401을 반환하고 기존 Refresh Token을 유지한다")
    void logout_withoutToken() throws Exception {
        createMember();
        login();
        String savedToken = storedMember().getRefreshToken();

        mockMvc.perform(delete("/users/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(header().doesNotExist("Set-Cookie"));

        assertEquals(savedToken, storedMember().getRefreshToken());
    }

    // 실제 JWT를 사용한 인증 실패
    @ParameterizedTest
    @ValueSource(strings = {"expired", "tampered", "malformed", "refresh", "unknownMember"})
    @DisplayName("유효하지 않은 Access Token으로 내 정보를 조회할 수 없다")
    void me_invalidToken(String kind) throws Exception {
        User user = createMember();
        MvcResult loggedIn = login();
        String savedToken = storedMember().getRefreshToken();

        String token = switch (kind) {
            case "expired" -> signedToken(accessSecret, user.getId(), true);
            case "tampered" -> tamper(loggedIn.getResponse().getCookie("accessToken").getValue());
            case "malformed" -> "invalid.jwt.token";
            case "refresh" -> savedToken;
            default -> signedToken(accessSecret, Long.MAX_VALUE, false);
        };

        mockMvc.perform(get("/users/me").cookie(new Cookie("accessToken", token)))
                .andExpect(status().is(kind.equals("unknownMember") ? 404 : 401))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value(kind.equals("unknownMember")
                        ? "USER_NOT_FOUND" : "INVALID_ACCESS_TOKEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertEquals(savedToken, storedMember().getRefreshToken());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic invalid", "invalidToken", "Bearer "})
    @DisplayName("잘못된 Authorization 헤더로 내 정보를 조회할 수 없다")
    void me_invalidHeader(String authorization) throws Exception {
        mockMvc.perform(get("/users/me").header("Authorization", authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(authorization.equals("Bearer ")
                        ? "UNAUTHORIZED" : "INVALID_AUTHORIZATION_HEADER"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // 회원가입 및 로그인 실패
    @Test
    @DisplayName("중복 가입은 409를 반환하고 기존 회원과 토큰을 변경하지 않는다")
    void join_duplicateEmail() throws Exception {
        createMember();
        login();
        User before = storedMember();
        long count = userRepository.count();

        mockMvc.perform(post("/users/join").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserJoinRequest(email, "different123", "다른 이름"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value(409))
                .andExpect(jsonPath("$.message").value("이미 존재하는 회원입니다."))
                .andExpect(header().doesNotExist("Set-Cookie"));

        User after = storedMember();
        assertEquals(count, userRepository.count());
        assertEquals(before.getId(), after.getId());
        assertEquals(before.getName(), after.getName());
        assertEquals(before.getPassword(), after.getPassword());
        assertEquals(before.getRefreshToken(), after.getRefreshToken());
    }

    @ParameterizedTest
    @MethodSource("invalidJoinFields")
    @DisplayName("가입 입력값 검증 실패 시 400을 반환하고 회원을 저장하지 않는다")
    void join_invalidInput(String field, String value, boolean omitted) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of(
                "email", email, "password", "password123", "name", "테스트 회원"));
        if (omitted) body.remove(field);
        else body.put(field, value);
        long count = userRepository.count();

        mockMvc.perform(post("/users/join").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value(400))
                .andExpect(jsonPath("$.data[*].field", org.hamcrest.Matchers.hasItem(field)))
                .andExpect(header().doesNotExist("Set-Cookie"));

        assertEquals(count, userRepository.count());
        assertTrue(userRepository.findByEmail(email).isEmpty());
    }

    static Stream<Arguments> invalidJoinFields() {
        return Stream.of(
                Arguments.of("name", null, true),
                Arguments.of("name", null, false),
                Arguments.of("email", "not-an-email", false),
                Arguments.of("password", "a".repeat(7), false),
                Arguments.of("password", "a".repeat(21), false)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 20})
    @DisplayName("가입 비밀번호 길이의 최솟값과 최댓값을 허용한다")
    void join_passwordBoundary(int length) throws Exception {
        String password = "a".repeat(length);
        mockMvc.perform(post("/users/join").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserJoinRequest(email, password, "테스트 회원"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email));

        assertTrue(passwordEncoder.matches(password, storedMember().getPassword()));
    }

    @ParameterizedTest
    @MethodSource("invalidLoginFields")
    @DisplayName("로그인 입력값 검증 실패 시 기존 인증 정보를 변경하지 않는다")
    void login_invalidInput(String field, String value, boolean omitted) throws Exception {
        createMember();
        login();
        String savedToken = storedMember().getRefreshToken();
        Map<String, Object> body = new HashMap<>(Map.of("email", email, "password", "password123"));
        if (omitted) body.remove(field);
        else body.put(field, value);
        mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value(400))
                .andExpect(jsonPath("$.data[*].field", org.hamcrest.Matchers.hasItem(field)))
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertEquals(savedToken, storedMember().getRefreshToken());
    }

    static Stream<Arguments> invalidLoginFields() {
        return Stream.concat(Stream.of("email", "password").flatMap(field -> Stream.of(
                        Arguments.of(field, null, true), Arguments.of(field, null, false),
                        Arguments.of(field, "", false), Arguments.of(field, "   ", false))),
                Stream.of(Arguments.of("email", "not-an-email", false)));
    }

    @ParameterizedTest
    @MethodSource("invalidJsonRequests")
    @DisplayName("가입과 로그인에서 잘못된 JSON 또는 본문 누락을 거부한다")
    void invalidJson(String path, String body) throws Exception {
        long count = userRepository.count();
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value(400))
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertEquals(count, userRepository.count());
    }

    static Stream<Arguments> invalidJsonRequests() {
        return Stream.of("/users/join", "/users/login")
                .flatMap(path -> Stream.of("", "{invalid", "null").map(body -> Arguments.of(path, body)));
    }

    @Test
    @DisplayName("비밀번호 불일치 시 토큰을 발급하지 않고 기존 인증 정보를 유지한다")
    void login_wrongPassword() throws Exception {
        createMember();
        login();
        User before = storedMember();
        mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserLoginRequest(email, "wrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value(401))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertEquals(before.getRefreshToken(), storedMember().getRefreshToken());
        assertEquals(before.getPassword(), storedMember().getPassword());
    }

    @Test
    @DisplayName("존재하지 않는 회원의 로그인은 404를 반환하고 회원을 생성하지 않는다")
    void login_memberNotFound() throws Exception {
        long count = userRepository.count();
        mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserLoginRequest(email, "password123"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value(404))
                .andExpect(jsonPath("$.message").value("존재하지 않는 회원입니다."))
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertEquals(count, userRepository.count());
        assertTrue(userRepository.findByEmail(email).isEmpty());
    }

    private void assertAuthenticationCookie(Cookie cookie, long expireMillis) {
        assertNotNull(cookie);
        assertFalse(cookie.getValue().isBlank());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
        assertEquals(expireMillis / 1000, cookie.getMaxAge());
    }

    private User createMember() {
        return userRepository.save(new User(email, passwordEncoder.encode("password123"), "테스트 회원"));
    }

    private User storedMember() {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserLoginRequest(email, "password123"))))
                .andExpect(status().isOk()).andReturn();
    }

    private void assertRefreshRejected(String token, int statusCode, String message) throws Exception {
        mockMvc.perform(post("/users/refresh").cookie(new Cookie("refreshToken", token)))
                .andExpect(status().is(statusCode))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(statusCode))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    // 테스트 전용 JWT 생성: 실제 서명 검증을 거치며 시간 대기 없이 만료 조건을 만든다.
    private String signedToken(String secret, Long userId, boolean expired) {
        Instant now = Instant.now();
        return Jwts.builder().claim("id", userId)
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(expired ? now.minusSeconds(60) : now.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String tamper(String token) {
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        return token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
    }
}
