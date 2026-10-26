package com.budzet.domain.user.controller;

import com.budzet.domain.user.dto.UserLoginRequest;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserPasswordIntegrationTest {

    private static final String CURRENT_PASSWORD = "password123";
    private static final String PATH = "/users/me/password";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String email = UUID.randomUUID() + "@test.com";

    @AfterEach
    void tearDown() {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 20})
    @DisplayName("비밀번호 변경 시 암호화 저장·인증 정리 후 새 비밀번호로만 로그인할 수 있다")
    void updatePassword_success(int length) throws Exception {
        MvcResult login = prepareLoggedInMember();
        User before = storedMember();
        String newPassword = "n".repeat(length);

        MvcResult result = mockMvc.perform(patch(PATH)
                        .cookie(login.getResponse().getCookie("accessToken"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(CURRENT_PASSWORD, newPassword)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(200))
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다. 다시 로그인해주세요."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        // 서비스 트랜잭션이 끝난 뒤 별도 조회하여 실제 저장을 확인한다.
        User after = storedMember();
        assertNotEquals(newPassword, after.getPassword());
        assertNotEquals(before.getPassword(), after.getPassword());
        assertTrue(passwordEncoder.matches(newPassword, after.getPassword()));
        assertFalse(passwordEncoder.matches(CURRENT_PASSWORD, after.getPassword()));
        assertNull(after.getRefreshToken());
        assertEquals(before.getId(), after.getId());
        assertEquals(before.getEmail(), after.getEmail());
        assertEquals(before.getName(), after.getName());
        for (String name : new String[]{"accessToken", "refreshToken"}) {
            Cookie cookie = result.getResponse().getCookie(name);
            assertNotNull(cookie);
            assertEquals("", cookie.getValue());
            assertEquals(0, cookie.getMaxAge());
            assertEquals("/", cookie.getPath());
            assertTrue(cookie.isHttpOnly());
            assertTrue(cookie.getSecure());
            assertEquals("Lax", cookie.getAttribute("SameSite"));
        }

        mockMvc.perform(post("/users/refresh")
                        .cookie(login.getResponse().getCookie("refreshToken")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertNull(storedMember().getRefreshToken());
        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());

        login(CURRENT_PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertNull(storedMember().getRefreshToken());
        login(newPassword).andExpect(status().isOk());
        assertNotNull(storedMember().getRefreshToken());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("현재 비밀번호 불일치 또는 동일한 새 비밀번호는 인증 정보 변경 없이 거부한다")
    void updatePassword_rejectedPassword(boolean samePassword) throws Exception {
        Cookie access = prepareLoggedInMember().getResponse().getCookie("accessToken");
        User before = storedMember();

        mockMvc.perform(patch(PATH).cookie(access).contentType(MediaType.APPLICATION_JSON)
                        .content(body(samePassword ? CURRENT_PASSWORD : "wrongPassword",
                                samePassword ? CURRENT_PASSWORD : "newPassword123")))
                .andExpect(status().is(samePassword ? 400 : 401))
                .andExpect(jsonPath("$.resultCode").value(samePassword ? 400 : 401))
                .andExpect(jsonPath("$.message").value(samePassword
                        ? "새 비밀번호는 현재 비밀번호와 달라야 합니다." : "비밀번호가 일치하지 않습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"));

        assertUnchanged(before);
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    @DisplayName("비밀번호 필수값·새 비밀번호 길이 검증 실패 시 DB와 쿠키를 변경하지 않는다")
    void updatePassword_invalidInput(String field, String value, boolean omitted) throws Exception {
        Cookie access = prepareLoggedInMember().getResponse().getCookie("accessToken");
        User before = storedMember();
        Map<String, Object> request = new HashMap<>(Map.of(
                "currentPassword", CURRENT_PASSWORD, "newPassword", "newPassword123"));
        if (omitted) request.remove(field);
        else request.put(field, value);

        mockMvc.perform(patch(PATH).cookie(access).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value(400))
                .andExpect(jsonPath("$.data[*].field", hasItem(field)))
                .andExpect(header().doesNotExist("Set-Cookie"));

        assertUnchanged(before);
    }

    static Stream<Arguments> invalidFields() {
        return Stream.concat(
                Stream.of("currentPassword", "newPassword").flatMap(field -> Stream.of(
                        Arguments.of(field, null, true),
                        Arguments.of(field, null, false),
                        Arguments.of(field, "", false),
                        Arguments.of(field, "   ", false))),
                Stream.of(Arguments.of("newPassword", "n".repeat(7), false),
                        Arguments.of("newPassword", "n".repeat(21), false)));
    }

    @Test
    @DisplayName("인증 없는 비밀번호 변경 요청은 거부하고 회원 정보를 유지한다")
    void updatePassword_withoutAuthentication() throws Exception {
        prepareLoggedInMember();
        User before = storedMember();

        mockMvc.perform(patch(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(body(CURRENT_PASSWORD, "newPassword123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(header().doesNotExist("Set-Cookie"));

        assertUnchanged(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{invalid", "null"})
    @DisplayName("비밀번호 변경 요청 본문이 없거나 잘못된 JSON이면 변경하지 않는다")
    void updatePassword_invalidJson(String request) throws Exception {
        Cookie access = prepareLoggedInMember().getResponse().getCookie("accessToken");
        User before = storedMember();
        mockMvc.perform(patch(PATH).cookie(access).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertUnchanged(before);
    }

    private MvcResult prepareLoggedInMember() throws Exception {
        userRepository.save(new User(email, passwordEncoder.encode(CURRENT_PASSWORD), "테스트 회원"));
        return login(CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginRequest(email, password))));
    }

    private String body(String currentPassword, String newPassword) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "currentPassword", currentPassword, "newPassword", newPassword));
    }

    private User storedMember() {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private void assertUnchanged(User before) {
        User after = storedMember();
        assertEquals(before.getPassword(), after.getPassword());
        assertEquals(before.getRefreshToken(), after.getRefreshToken());
        assertEquals(before.getName(), after.getName());
        assertEquals(before.getEmail(), after.getEmail());
    }
}
