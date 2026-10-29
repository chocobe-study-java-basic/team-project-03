package com.budzet.global.config;

import com.budzet.domain.room.entity.Authority;
import com.budzet.domain.room.entity.Currency;
import com.budzet.domain.room.repository.RoomRepository;
import com.budzet.domain.room.repository.UserRoomConnectionRepository;
import com.budzet.domain.user.repository.UserRepository;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=",
        "spring.config.import=classpath:application-test.yaml",
        "spring.datasource.url=jdbc:h2:mem:support_config;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@Transactional
class SupportConfigurationIntegrationTest {
    @Autowired private TestBedInitializer initializer;
    @Autowired private UserRepository users;
    @Autowired private RoomRepository rooms;
    @Autowired private UserRoomConnectionRepository connections;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void startupSeedsUsersRoomsAndMemberships() {
        assertThat(users.count()).isEqualTo(21);
        for (int number = 1; number <= 21; number++) {
            var user = users.findByEmail("user" + number + "@example.com");
            assertThat(user).isNotNull();
            assertThat(user.getName()).isEqualTo("테스트 사용자 " + number);
            assertThat(passwordEncoder.matches("12341234", user.getPassword())).isTrue();
        }
        assertThat(rooms.findAll()).extracting(
                room -> room.getName(), room -> room.getTotalBudget(),
                room -> room.getAvailableBudget(), room -> room.getCurrency())
                .containsExactlyInAnyOrder(
                        tuple("프로젝트 회식", 500_000L, 500_000L, Currency.KRW),
                        tuple("일본 여행", 100_000L, 100_000L, Currency.JPY),
                        tuple("미국 여행", 10_000L, 10_000L, Currency.USD));
        var memberships = connections.findAll();
        assertThat(memberships).hasSize(18).allSatisfy(connection -> {
            assertThat(connection.isJoined()).isTrue();
            assertThat(connection.getCreatedAt()).isNotNull();
            int number = Integer.parseInt(connection.getUser().getEmail().split("@")[0].substring(4));
            String expectedRoom = number <= 10 ? "프로젝트 회식" : number <= 15 ? "일본 여행" : "미국 여행";
            assertThat(connection.getRoom().getName()).isEqualTo(expectedRoom);
            assertThat(connection.getAuthority()).isEqualTo(
                    List.of(1, 11, 16).contains(number) ? Authority.OWNER : Authority.MEMBER);
        });
        assertThat(memberships).extracting(connection -> connection.getUser().getEmail())
                .containsExactlyInAnyOrderElementsOf(java.util.stream.IntStream.rangeClosed(1, 18)
                        .mapToObj(number -> "user" + number + "@example.com").toList());
    }

    @Test
    void reseedingWithExistingDataPreservesIdsAndUserChanges() {
        var user = users.findByEmail("user1@example.com");
        assertThat(user).isNotNull();
        user.updateName("수정한 사용자");
        users.flush();
        var userIds = users.findAll().stream().map(value -> value.getId()).toList();
        var roomIds = rooms.findAll().stream().map(value -> value.getId()).toList();
        initializer.seed();
        assertThat(users.findAll()).extracting(value -> value.getId()).containsExactlyInAnyOrderElementsOf(userIds);
        assertThat(rooms.findAll()).extracting(value -> value.getId()).containsExactlyInAnyOrderElementsOf(roomIds);
        assertThat(connections.count()).isEqualTo(18);
        var updatedUser = users.findByEmail("user1@example.com");
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getName()).isEqualTo("수정한 사용자");
    }

    @Test
    void swaggerPublishesAllGroupsWithBearerScheme() throws Exception {
        String config = mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        var urls = mapper.readTree(config).path("urls");
        assertThat(urls.size()).isEqualTo(5);
        var expected = List.of("00. 전체 API", "01. 회원 & 인증", "02. 모임방 & 맴버",
                "03. 예산관리 & 예산 신청 & 정산", "04. 초대");
        for (int index = 0; index < urls.size(); index++) {
            var group = urls.get(index);
            assertThat(group.path("name").asText()).isEqualTo(expected.get(index));
            String path = "/v3/api-docs/" + URLEncoder.encode(expected.get(index), StandardCharsets.UTF_8).replace("+", "%20");
            String json = mvc.perform(get(URI.create(path)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title").value("Budzet API 서버"))
                    .andExpect(jsonPath("$.info.version").value("beta"))
                    .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.type").value("http"))
                    .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.scheme").value("bearer"))
                    .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.bearerFormat").value("JWT"))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            var paths = mapper.readTree(json).path("paths");
            assertThat(paths.size()).isPositive();
            for (String route : paths.propertyNames()) {
                assertThat(switch (index) {
                    case 0 -> true;
                    case 1 -> route.startsWith("/users/");
                    case 2 -> route.equals("/rooms") || route.equals("/rooms/{roomId}") || route.startsWith("/rooms/{roomId}/members");
                    case 3 -> route.startsWith("/rooms/{roomId}/budget");
                    case 4 -> route.startsWith("/invites/") || route.startsWith("/rooms/{roomId}/invites") || route.startsWith("/rooms/join/");
                    default -> false;
                }).as("group %s route %s", expected.get(index), route).isTrue();
            }
        }
    }

    @Test
    void corsAllowsFrontendPreflightAndRejectsUnknownOrigins() throws Exception {
        mvc.perform(options("/rooms").header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/rooms").header("Origin", "https://unknown.example")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
