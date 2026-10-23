package com.budzet.domain.budget.controller;

import com.budzet.domain.budget.dto.BudgetRequestResponse;
import com.budzet.domain.budget.service.BudgetRequestService;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.service.UserService;
import com.budzet.global.rq.Rq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class BudgetRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetRequestService budgetRequestService;

    @MockitoBean
    private Rq rq;

    @MockitoBean
    private UserService userService;

    private User user;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;


    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(rq.getActor()).thenReturn(user);
    }

    @Test
    @DisplayName("예산 신청 등록 성공")
    void registration_success() throws Exception {
        mockMvc.perform(post("/rooms/{roomId}/budget/request", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "운영비 신청",
                                    "requested_amount": 10000
                                }
                                """))
                .andExpect(status().isCreated());

        verify(budgetRequestService).budgetRequestRegistration(
                1L,
                user,
                "운영비 신청",
                10000L
        );
    }

    @Test
    @DisplayName("예산 신청 등록 실패 - 유효성 검증")
    void registration_invalidRequest() throws Exception {
        mockMvc.perform(post("/rooms/{roomId}/budget/request", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "",
                                    "requested_amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예산 신청 목록 조회 성공")
    void getBudgetRequestList_success() throws Exception {
        when(budgetRequestService.getBudgetRequestList(1L, user))
                .thenReturn(Collections.<BudgetRequestResponse>emptyList());

        mockMvc.perform(get("/rooms/{roomId}/budget/request/list", 1L))
                .andExpect(status().isOk());

        verify(budgetRequestService).getBudgetRequestList(1L, user);
    }

    @Test
    @DisplayName("예산 신청 상세 조회 성공")
    void getBudgetRequest_success() throws Exception {
        mockMvc.perform(get(
                        "/rooms/{roomId}/budget/request/{requestId}",
                        1L,
                        10L
                ))
                .andExpect(status().isOk());

        verify(budgetRequestService).getBudgetRequest(1L, user, 10L);
    }

    @Test
    @DisplayName("예산 신청 삭제 성공")
    void deleteBudgetRequest_success() throws Exception {
        mockMvc.perform(delete(
                        "/rooms/{roomId}/budget/request/{requestId}",
                        1L,
                        10L
                ))
                .andExpect(status().isNoContent());

        verify(budgetRequestService).deleteBudgetRequest(
                1L,
                user,
                10L
        );
    }

    @Test
    @DisplayName("예산 신청 승인 성공")
    void approveBudgetRequest_success() throws Exception {
        mockMvc.perform(patch(
                        "/rooms/{roomId}/budget/request/{requestId}/approve",
                        1L,
                        10L
                ))
                .andExpect(status().isNoContent());

        verify(budgetRequestService).approveBudgetRequest(
                1L,
                user,
                10L
        );
    }

    @Test
    @DisplayName("예산 신청 반려 성공")
    void rejectBudgetRequest_success() throws Exception {
        mockMvc.perform(patch(
                        "/rooms/{roomId}/budget/request/{requestId}/reject",
                        1L,
                        10L
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "rejectReason": "예산 근거 부족"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(budgetRequestService).rejectBudgetRequest(
                1L,
                user,
                10L,
                "예산 근거 부족"
        );
    }

    @Test
    @DisplayName("예산 신청 수정 성공")
    void modifyBudgetRequest_success() throws Exception {
        mockMvc.perform(patch(
                        "/rooms/{roomId}/budget/request/{requestId}/modify",
                        1L,
                        10L
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "수정된 운영비 신청",
                                    "rerequestedAmount": 20000
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(budgetRequestService).modifyBudgetRequest(
                1L,
                user,
                10L,
                "수정된 운영비 신청",
                20000L
        );
    }

    @Test
    @DisplayName("예산 신청 수정 실패 - 사유 누락")
    void modifyBudgetRequest_invalidReason() throws Exception {
        mockMvc.perform(patch(
                        "/rooms/{roomId}/budget/request/{requestId}/modify",
                        1L,
                        10L
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "",
                                    "rerequestedAmount": 20000
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예산 신청 반려 실패 - 반려 사유 길이 초과")
    void rejectBudgetRequest_invalidReason() throws Exception {
        String longReason = "가".repeat(51);

        mockMvc.perform(patch(
                        "/rooms/{roomId}/budget/request/{requestId}/reject",
                        1L,
                        10L
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "rejectReason": "%s"
                                }
                                """.formatted(longReason)))
                .andExpect(status().isBadRequest());
    }
}