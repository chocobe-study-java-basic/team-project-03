package com.budzet.domain.budget.controller

import com.budzet.domain.budget.dto.*
import com.budzet.domain.budget.service.BudgetChangeService
import com.budzet.global.api.ApiResponse
import com.budzet.global.rq.Rq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "03 - 3. 정산", description = "정산처리, 정산 목록/상세 조회 및 수정 API")
@RestController
@RequestMapping("/rooms")
class BudgetChangeController(
    private val rq: Rq,
    private val budgetChangeService: BudgetChangeService,
) {
    @Operation(
        summary = "정산 처리",
        description = "신청자 본인만 가능",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @PostMapping("/{roomId}/budget/changes/{requestId}")
    fun createBudgetChange(
        @PathVariable roomId: Long,
        @PathVariable requestId: Long,
        @RequestBody @Valid request: BudgetChangeCreateRequest
    ): ResponseEntity<ApiResponse<BudgetChangeCreateResponse>> {
        val user = rq.getActor()

        val response = budgetChangeService.createBudgetChange(
            roomId, requireNotNull(user.id), requestId, request)

        return ApiResponse.response(HttpStatus.CREATED,"정산 처리 성공",response)

    }

    @Operation(
        summary = "정산 내역 목록 조회",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @GetMapping("/{roomId}/budget/changes/settlement")
    fun budgetChangeList(
        @PathVariable roomId: Long
    ): ResponseEntity<ApiResponse<BudgetChangeListResponse>> {
        val user = rq.getActor()
        val response = budgetChangeService.budgetChangeList(roomId, requireNotNull(user.id))
        return ApiResponse.response(HttpStatus.OK,"정산 내역 조회 성공",response)
    }

    @Operation(
        summary = "정산 내역 상세 조회",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @GetMapping("/{roomId}/budget/changes/{changeId}")
    fun budgetChangeDetail(
        @PathVariable roomId: Long,
        @PathVariable changeId: Long
    ): ResponseEntity<ApiResponse<BudgetChangeDetailResponse>> {
        val user = rq.getActor()
        val response = budgetChangeService.budgetChangeDetail(roomId, requireNotNull(user.id), changeId)
        return ApiResponse.response(HttpStatus.OK,"정산 내역 상세조회 성공",response)
    }

    @Operation(
        summary = "정산 내역 수정",
        description = "모임장 권한 필요",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @PutMapping("/{roomId}/budget/changes/{changeId}")
    fun updateBudgetChange(
        @PathVariable roomId: Long,
        @PathVariable changeId: Long,
        @RequestBody @Valid request: BudgetChangeUpdateRequest
    ): ResponseEntity<ApiResponse<BudgetChangeUpdateResponse>> {
        val user = rq.getActor()
        val response = budgetChangeService.updateBudgetChange(
            roomId, requireNotNull(user.id), changeId, request)

        return ApiResponse.response(HttpStatus.OK,"정산 내역 수정 성공",response)
    }
}
