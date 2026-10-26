package com.budzet.global.actuator;

import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.repository.UserRepository;
import com.budzet.global.config.TestBedInitializer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

// 별도 프로필 없이 실제 개발 환경 조건을 적용하고 DB·JWT 값만 테스트용으로 대체한다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=",
        "spring.config.import=classpath:application-test.yaml",
        "spring.datasource.url=jdbc:h2:mem:actuator_dev;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class ActuatorIntegrationTest {

    private static final String PASSWORD = "password123";
    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebEndpointsSupplier endpointsSupplier;

    @Autowired
    private Environment environment;

    // 개발 데이터 초기화는 이번 검증 대상이 아니므로 실행하지 않는다.
    @MockitoBean
    private TestBedInitializer testBedInitializer;

    private Long userId;
    private String accessToken;

    @BeforeEach
    void login() throws Exception {
        String email = UUID.randomUUID() + "@test.com";
        User user = userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), "지표 확인"));
        userId = user.getId();
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/users/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", PASSWORD))))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        accessToken = objectMapper.readTree(response.body()).path("data").path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
    }

    @AfterEach
    void tearDown() {
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    @DisplayName("프로필을 지정하지 않은 개발 환경에서 상태와 지표만 노출한다")
    void developmentEndpointsEnabledWithoutProfile() {
        assertThat(environment.getActiveProfiles()).isEmpty();
        assertThat(endpointsSupplier.getEndpoints())
                .extracting(endpoint -> endpoint.getEndpointId().toString())
                .containsExactlyInAnyOrder("health", "metrics");
    }

    @Test
    @DisplayName("실제 JWT로 상태를 조회하고 내부 상세 정보는 공개하지 않는다")
    void health() throws Exception {
        HttpResponse<String> response = get("/actuator/health", accessToken);
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.has("components")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"jvm.memory.used", "jvm.threads.live"})
    @DisplayName("JVM 메모리와 스레드 지표를 조회한다")
    void jvmMetrics(String name) throws Exception {
        HttpResponse<String> response = get("/actuator/metrics/" + name, accessToken);
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("name").asText()).isEqualTo(name);
        assertThat(body.path("measurements").get(0).path("value").asDouble()).isPositive();
    }

    @Test
    @DisplayName("Room 조회 후 HTTP 요청 수가 증가하고 누적 처리 시간이 기록된다")
    void roomRequestsAreMeasured() throws Exception {
        assertThat(get("/rooms", accessToken).statusCode()).isEqualTo(200);
        JsonNode before = roomMetric();
        for (int i = 0; i < 3; i++) {
            assertThat(get("/rooms", accessToken).statusCode()).isEqualTo(200);
        }
        JsonNode after = roomMetric();
        assertThat(measurement(after, "COUNT") - measurement(before, "COUNT")).isEqualTo(3.0);
        assertThat(measurement(after, "TOTAL_TIME")).isGreaterThan(measurement(before, "TOTAL_TIME"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/metrics", "/rooms"})
    @DisplayName("관리 경로와 기존 Room API는 미인증 요청을 거부한다")
    void authenticationRequired(String path) throws Exception {
        assertThat(get(path, null).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("잘못된 JWT로 관리 지표를 조회할 수 없다")
    void invalidTokenRejected() throws Exception {
        assertThat(get("/actuator/metrics", "invalid-token").statusCode()).isEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/env", "/actuator/beans", "/actuator/prometheus"})
    @DisplayName("허용하지 않은 관리 엔드포인트는 로그인해도 노출되지 않는다")
    void otherEndpointsNotExposed(String path) throws Exception {
        assertThat(endpointsSupplier.getEndpoints())
                .extracting(endpoint -> endpoint.getEndpointId().toString())
                .containsExactlyInAnyOrder("health", "metrics");
        // 미등록 경로의 오류 디스패치도 기존 Security 설정을 거친다.
        // 401/404와 별개로 실제 노출 목록에 해당 엔드포인트가 없는지 위에서 검증한다.
        assertThat(get(path, accessToken).statusCode()).isIn(401, 404);
    }

    private JsonNode roomMetric() throws Exception {
        HttpResponse<String> response = get(
                "/actuator/metrics/http.server.requests?tag=uri:/rooms&tag=method:GET&tag=status:200",
                accessToken);
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body());
    }

    private double measurement(JsonNode metric, String statistic) {
        for (JsonNode measurement : metric.path("measurements")) {
            if (statistic.equals(measurement.path("statistic").asText())) {
                return measurement.path("value").asDouble();
            }
        }
        throw new AssertionError("지표에 " + statistic + " 값이 없습니다.");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}

@SpringBootTest
@ActiveProfiles("test")
class ActuatorDisabledIntegrationTest {

    @Autowired
    private WebEndpointsSupplier endpointsSupplier;

    @Test
    @DisplayName("test 프로필에서는 관리 엔드포인트를 활성화하지 않는다")
    void endpointsDisabledInTest() {
        assertThat(endpointsSupplier.getEndpoints()).isEmpty();
    }
}

@SpringBootTest(properties = {
        "spring.config.import=classpath:application-test.yaml",
        "spring.datasource.url=jdbc:h2:mem:actuator_prod;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@ActiveProfiles("prod")
class ActuatorProductionDisabledIntegrationTest {

    @Autowired
    private WebEndpointsSupplier endpointsSupplier;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("prod 프로필에서는 관리 엔드포인트를 활성화하지 않는다")
    void endpointsDisabledInProduction() {
        assertThat(environment.getActiveProfiles()).containsExactly("prod");
        assertThat(endpointsSupplier.getEndpoints()).isEmpty();
    }
}
