package com.budzet.domain.budget.controller

import com.budzet.domain.budget.dto.BudgetRequestModifyRequest
import com.budzet.domain.budget.dto.BudgetRequestRejectRequest
import com.budzet.domain.budget.dto.BudgetRequestRequest
import com.budzet.domain.budget.dto.BudgetRequestResponse
import com.budzet.domain.budget.service.BudgetRequestService
import com.budzet.global.api.ApiResponse
import com.budzet.global.rq.Rq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "03 - 2. 예산 신청", description = "예산 신청 등록, 목록/상세 조회, 삭제, 승인, 반려 및 수정 API")
@RestController
@RequestMapping("/rooms")
class BudgetRequestController(
    private val rq: Rq,
    private val budgetRequestService: BudgetRequestService,
) {
    @Operation(
        summary = "예산 신청 등록",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @PostMapping("/{roomId}/budget/request")
    fun postBudgetRequest(
        @PathVariable roomId: Long,
        @Valid @RequestBody budgetRequestRequest: BudgetRequestRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val user = rq.getActor()
        budgetRequestService.budgetRequestRegistration(roomId, user, budgetRequestRequest.reason, budgetRequestRequest.requested_amount)
        return ApiResponse.response(HttpStatus.CREATED, "예산신청 등록성공", null)
    }

    @Operation(
        summary = "예산 신청 목록 조회",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @GetMapping("/{roomId}/budget/request/list")
    fun getBudgetRequestList(
        @PathVariable roomId: Long
    ): ResponseEntity<ApiResponse<List<BudgetRequestResponse>>> {
        val user = rq.getActor()
        return ApiResponse.response(HttpStatus.OK, "예산신청 목록조회성공", budgetRequestService.getBudgetRequestList(roomId, user))
    }

    @Operation(
        summary = "예산 신청 상세 조회",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @GetMapping("/{roomId}/budget/request/{requestId}")
    fun getBudgetRequest(
        @PathVariable roomId: Long,
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<BudgetRequestResponse>> {
        val user = rq.getActor()
        return ApiResponse.response(HttpStatus.OK, "예산신청 조회성공", budgetRequestService.getBudgetRequest(roomId, user, requestId))
    }

    @Operation(
        summary = "예산 신청 삭제",
        description = "신청자 본인만 가능",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @DeleteMapping("/{roomId}/budget/request/{requestId}")
    fun deleteBudgetRequest(
        @PathVariable roomId: Long,
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<BudgetRequestResponse>> {
        val user = rq.getActor()
        budgetRequestService.deleteBudgetRequest(roomId, user, requestId)
        return ApiResponse.response(HttpStatus.NO_CONTENT, "예산신청 삭제성공", null)
    }

    @Operation(
        summary = "예산 신청 승인",
        description = "모임장, 운영자 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @PatchMapping("/{roomId}/budget/request/{requestId}/approve")
    fun approveBudgetRequest(
        @PathVariable roomId: Long,
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<Any>> {
        val user = rq.getActor()
        budgetRequestService.approveBudgetRequest(roomId, user, requestId)
        return ApiResponse.response(HttpStatus.NO_CONTENT, "예산신청 승인성공", null)
    }

    @Operation(
        summary = "예산 신청 반려",
        description = "모임장, 운영자 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @PatchMapping("/{roomId}/budget/request/{requestId}/reject")
    fun rejectBudgetRequest(
        @PathVariable roomId: Long,
        @PathVariable requestId: Long,
        @Valid @RequestBody request: BudgetRequestRejectRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val user = rq.getActor()
        budgetRequestService.rejectBudgetRequest(roomId, user, requestId, request.rejectReason)
        return ApiResponse.response(HttpStatus.NO_CONTENT, "예산신청 반려성공", null)
    }

    @Operation(
        summary = "예산 신청 수정",
        description = "신청자 본인만 가능, 정산 처리된 신청 건은 수정 불가",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @PatchMapping("/{roomId}/budget/request/{requestId}/modify")
    fun modifyBudgetRequest(
        @PathVariable roomId: Long,
        @PathVariable requestId: Long,
        @Valid @RequestBody request: BudgetRequestModifyRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val user = rq.getActor()
        budgetRequestService.modifyBudgetRequest(roomId, user, requestId, request.reason, request.rerequestedAmount)
        return ApiResponse.response(HttpStatus.NO_CONTENT, "예산신청 수정성공", null)
    }

}
