
package com.budzet.domain.budget.controller;

import com.budzet.domain.budget.entity.BudgetRequest;
import com.budzet.domain.budget.repository.BudgetRequestRepository;
import com.budzet.domain.room.entity.Currency;
import com.budzet.domain.room.entity.Room;
import com.budzet.domain.room.entity.UserRoomConnection;
import com.budzet.domain.room.repository.RoomRepository;
import com.budzet.domain.room.repository.UserRoomConnectionRepository;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BudgetRequestIntegrationTest {

    private static final String PASSWORD = "test1234";

    private static final long INITIAL_BUDGET = 100_000L;
    private static final long REQUESTED_AMOUNT = 15_000L;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserService userService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRoomConnectionRepository userRoomConnectionRepository;

    @Autowired
    private BudgetRequestRepository budgetRequestRepository;

    private Room room;

    private String ownerToken;
    private String applicantToken;

    @BeforeEach
    void setUp() throws Exception {
        User owner = userService.join(
                "owner@test.com",
                PASSWORD,
                "테스트 방장"
        );

        User applicant = userService.join(
                "applicant@test.com",
                PASSWORD,
                "테스트 신청자"
        );

        ownerToken = loginAndGetAccessToken("owner@test.com");
        applicantToken = loginAndGetAccessToken("applicant@test.com");

        room = Room.create(
                "예산 신청 통합 테스트 방",
                INITIAL_BUDGET,
                Currency.KRW
        );

        room = roomRepository.save(room);

        userRoomConnectionRepository.save(
                UserRoomConnection.createOwner(owner, room)
        );

        userRoomConnectionRepository.save(
                UserRoomConnection.createMember(applicant, room)
        );
    }

    private String loginAndGetAccessToken(String email) throws Exception {
        String response = mockMvc.perform(
                        post("/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of(
                                                "email", email,
                                                "password", PASSWORD
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        JsonNode accessTokenNode = findField(root, "accessToken");

        assertNotNull(
                accessTokenNode,
                "로그인 응답에 accessToken이 없습니다."
        );

        assertFalse(
                accessTokenNode.asText().isBlank(),
                "accessToken이 비어 있습니다."
        );

        return accessTokenNode.asText();
    }

    private JsonNode findField(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }

        if (node.isObject()) {
            JsonNode value = node.get(fieldName);

            if (value != null && value.isValueNode()) {
                return value;
            }

            // fields() 대신 properties()를 사용하고, 향상된 for문으로 순회합니다.
            for (Map.Entry<String, JsonNode> property : node.properties()) {
                JsonNode found = findField(
                        property.getValue(),
                        fieldName
                );

                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findField(child, fieldName);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }


    private Long createBudgetRequest() throws Exception {
        mockMvc.perform(
                        post("/rooms/{roomId}/budget/request", room.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + applicantToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of(
                                                "reason", "테스트 예산 신청",
                                                "requested_amount",
                                                REQUESTED_AMOUNT
                                        )
                                ))
                )
                .andExpect(status().isCreated());

        return budgetRequestRepository.findAll()
                .stream()
                .map(BudgetRequest::getId)
                .max(Comparator.naturalOrder())
                .orElseThrow(() ->
                        new AssertionError(
                                "예산 신청 데이터가 저장되지 않았습니다."
                        )
                );
    }

    @Test
    @DisplayName("신청자는 예산 신청을 등록하고 목록을 조회할 수 있다")
    void shouldRegisterBudgetRequestAndRetrieveList() throws Exception {
        createBudgetRequest();

        mockMvc.perform(
                        get(
                                "/rooms/{roomId}/budget/request/list",
                                room.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + applicantToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("신청자는 예산 신청 상세 정보를 조회할 수 있다")
    void shouldRetrieveBudgetRequestDetails() throws Exception {
        Long requestId = createBudgetRequest();

        mockMvc.perform(
                        get(
                                "/rooms/{roomId}/budget/request/{requestId}",
                                room.getId(),
                                requestId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + applicantToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("방장은 예산 신청을 승인할 수 있다")
    void shouldApproveBudgetRequest() throws Exception {
        Long requestId = createBudgetRequest();

        mockMvc.perform(
                        patch(
                                "/rooms/{roomId}/budget/request/{requestId}/approve",
                                room.getId(),
                                requestId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                )
                .andExpect(status().isNoContent());

        Room updatedRoom = roomRepository.findById(room.getId())
                .orElseThrow();

        assertEquals(
                INITIAL_BUDGET - REQUESTED_AMOUNT,
                updatedRoom.getAvailableBudget()
        );
    }

    @Test
    @DisplayName("방장은 예산 신청을 반려할 수 있다")
    void shouldRejectBudgetRequest() throws Exception {
        Long requestId = createBudgetRequest();

        mockMvc.perform(
                        patch(
                                "/rooms/{roomId}/budget/request/{requestId}/reject",
                                room.getId(),
                                requestId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of(
                                                "rejectReason",
                                                "예산 사용 목적 확인 필요"
                                        )
                                ))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("신청자는 본인의 예산 신청을 수정할 수 있다")
    void shouldModifyOwnBudgetRequest() throws Exception {
        Long requestId = createBudgetRequest();

        mockMvc.perform(
                        patch(
                                "/rooms/{roomId}/budget/request/{requestId}/modify",
                                room.getId(),
                                requestId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + applicantToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of(
                                                "reason", "수정된 예산 신청 사유",
                                                "rerequestedAmount", 20_000L
                                        )
                                ))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("신청자는 본인의 예산 신청을 삭제할 수 있다")
    void shouldDeleteOwnBudgetRequest() throws Exception {
        Long requestId = createBudgetRequest();

        mockMvc.perform(
                        delete(
                                "/rooms/{roomId}/budget/request/{requestId}",
                                room.getId(),
                                requestId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + applicantToken
                                )
                )
                .andExpect(status().isNoContent());

        assertFalse(
                budgetRequestRepository.findById(requestId).isPresent(),
                "삭제된 예산 신청이 DB에 남아 있습니다."
        );
    }

    @Test
    @DisplayName("인증 토큰이 없으면 예산 신청 목록 조회가 거부된다")
    void shouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(
                        get(
                                "/rooms/{roomId}/budget/request/list",
                                room.getId()
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("신청 금액이 0이면 등록 요청이 거부된다")
    void shouldRejectBudgetRequestWithZeroAmount() throws Exception {
        mockMvc.perform(
                        post("/rooms/{roomId}/budget/request", room.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + applicantToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of(
                                                "reason", "잘못된 금액 테스트",
                                                "requested_amount", 0
                                        )
                                ))
                )
                .andExpect(status().isBadRequest());
    }
}
