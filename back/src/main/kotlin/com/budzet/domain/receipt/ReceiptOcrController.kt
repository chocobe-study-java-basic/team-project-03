package com.budzet.domain.receipt

import com.budzet.global.api.ApiResponse
import com.budzet.global.rq.Rq
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/rooms/{roomId}/budget/requests/{requestId}/receipt")
class ReceiptOcrController(private val rq: Rq, private val service: ReceiptOcrService) {
    @PostMapping("/analyze", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun analyze(@PathVariable roomId: Long, @PathVariable requestId: Long, @RequestPart("file") file: MultipartFile) =
        ApiResponse.response(HttpStatus.OK, "영수증 분석 완료",
            service.analyze(roomId, requestId, requireNotNull(rq.getActor().id), file))
}
