package com.budzet.domain.budget.dto;

import com.budzet.domain.budget.entity.BudgetRequestType;

import java.time.LocalDateTime;

public record BudgetRequestResponse(
        Long id,
        Long roomId,
        Long userId,
        String userName,
        String reason,
        Long requestedAmount,
        BudgetRequestType status,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}
