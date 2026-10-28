package com.budzet.domain.receipt

import com.budzet.domain.budget.dto.*
import com.budzet.domain.budget.service.*
import com.budzet.domain.budget.repository.BudgetChangeRepository
import com.budzet.global.exception.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.Base64

data class ReceiptEvidenceResponse(val contentType: String, val base64: String)

@Service
class ReceiptEvidenceService(
    private val changes: BudgetChangeService,
    private val budgets: BudgetService,
    private val changeRepository: BudgetChangeRepository,
    private val evidence: ReceiptEvidenceRepository,
) {
    // Image and settlement commit or roll back together. OCR is not called here.
    @Transactional
    fun settle(roomId: Long, userId: Long, requestId: Long, request: BudgetChangeCreateRequest, file: MultipartFile): BudgetChangeCreateResponse {
        val bytes = ReceiptImageValidator.validate(file)
        val response = changes.createBudgetChange(roomId, userId, requestId, request)
        val change = changeRepository.getReferenceById(requireNotNull(response.id))
        evidence.save(ReceiptEvidence(change, requireNotNull(file.contentType), bytes))
        return response
    }

    @Transactional(readOnly = true)
    fun get(roomId: Long, userId: Long, changeId: Long): ReceiptEvidenceResponse? {
        budgets.validateRoomMember(roomId, userId)
        val change = changeRepository.findById(changeId)
            .orElseThrow { BusinessException(ErrorCode.BUDGET_CHANGE_NOT_FOUND) }
        if (change.room.id != roomId) throw BusinessException(ErrorCode.FORBIDDEN_ACCESS)
        return evidence.findByChange_Id(changeId)?.let {
            ReceiptEvidenceResponse(it.contentType, Base64.getEncoder().encodeToString(it.image))
        }
    }
}
