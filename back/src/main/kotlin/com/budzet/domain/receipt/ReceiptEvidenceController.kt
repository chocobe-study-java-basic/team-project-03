package com.budzet.domain.receipt

import com.budzet.domain.budget.dto.*
import com.budzet.global.api.ApiResponse
import com.budzet.global.rq.Rq
import jakarta.validation.Valid
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/rooms/{roomId}/budget")
class ReceiptEvidenceController(private val rq: Rq, private val service: ReceiptEvidenceService) {
    @PostMapping("/changes/{requestId}/with-receipt", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun settle(@PathVariable roomId: Long, @PathVariable requestId: Long,
        @RequestPart("request") @Valid request: BudgetChangeCreateRequest,
        @RequestPart("file") file: MultipartFile) = ApiResponse.response(HttpStatus.CREATED, "영수증 증빙과 정산이 저장되었습니다.",
            service.settle(roomId, requireNotNull(rq.getActor().id), requestId, request, file))

    @GetMapping("/changes/{changeId}/receipt")
    fun get(@PathVariable roomId: Long, @PathVariable changeId: Long): ResponseEntity<ApiResponse<ReceiptEvidenceResponse>> =
        ApiResponse.response(HttpStatus.OK, "정산 증빙 조회", service.get(roomId, requireNotNull(rq.getActor().id), changeId))
}
