package com.budzet.global.actuator;

import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.repository.UserRepository;
import com.budzet.global.config.TestBedInitializer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=",
        "spring.config.import=classpath:application-test.yaml",
        "spring.datasource.url=jdbc:h2:mem:prometheus_dev;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "custom.monitoring.password=test-scrape-password"
})
class PrometheusIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TestBedInitializer testBedInitializer;

    private Long userId;
    private String accessToken;

    @BeforeEach
    void login() throws Exception {
        String email = UUID.randomUUID() + "@test.com";
        User user = userRepository.save(new User(email, passwordEncoder.encode("password123"), "수집 검증"));
        userId = user.getId();
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/users/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", "password123")))));
        assertThat(response.statusCode()).isEqualTo(200);
        accessToken = objectMapper.readTree(response.body()).path("data").path("accessToken").asText();
    }

    @AfterEach
    void tearDown() {
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    @DisplayName("전용 Basic 인증으로 Prometheus 형식의 JVM·HTTP 지표를 읽는다")
    void scrape() throws Exception {
        HttpResponse<String> response = get("/actuator/prometheus", basic("prometheus", "test-scrape-password"));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).contains("text/plain");
        assertThat(response.body()).contains("jvm_memory_used_bytes", "http_server_requests_seconds_count");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "wrong-password", "wrong-user", "jwt", "jwt-cookie"})
    @DisplayName("수집 경로는 미인증·잘못된 계정과 사용자 JWT를 거부한다")
    void rejectOtherCredentials(String type) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri("/actuator/prometheus")).GET();
        switch (type) {
            case "wrong-password" -> request.header("Authorization", basic("prometheus", "wrong"));
            case "wrong-user" -> request.header("Authorization", basic("other", "test-scrape-password"));
            case "jwt" -> request.header("Authorization", "Bearer " + accessToken);
            case "jwt-cookie" -> request.header("Cookie", "accessToken=" + accessToken);
            default -> { }
        }
        assertThat(send(request).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("수집 전용 인증으로 HEAD 조회가 가능하고 본문은 반환하지 않는다")
    void scrapeHead() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/actuator/prometheus"))
                .header("Authorization", basic("prometheus", "test-scrape-password"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEmpty();
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    }

    @Test
    @DisplayName("브라우저의 잘못된 JWT 쿠키가 수집 전용 인증을 방해하지 않는다")
    void ignoreJwtCookieOnScrape() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/actuator/prometheus"))
                .header("Authorization", basic("prometheus", "test-scrape-password"))
                .header("Cookie", "accessToken=invalid-token").GET());
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/rooms", "/users/me", "/actuator/health", "/actuator/metrics"})
    @DisplayName("수집 계정으로 기존 API와 관리 조회에 접근할 수 없고 JWT 접근은 유지된다")
    void credentialsDoNotCrossChains(String path) throws Exception {
        assertThat(get(path, basic("prometheus", "test-scrape-password")).statusCode()).isEqualTo(401);
        assertThat(get(path, "Bearer " + accessToken).statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("수집 전용 인증은 상태 변경 요청을 허용하지 않는다")
    void rejectWrite() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/actuator/prometheus"))
                .header("Authorization", basic("prometheus", "test-scrape-password"))
                .POST(HttpRequest.BodyPublishers.noBody()));
        assertThat(response.statusCode()).isEqualTo(403);
    }

    private String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).header("Authorization", authorization).GET());
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
