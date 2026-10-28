package com.budzet.domain.receipt

import com.budzet.global.exception.BusinessException
import com.budzet.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

interface ReceiptOcrClient {
    fun recognize(bytes: ByteArray, contentType: String): String
}

@Component
class OcrSpaceClient(@Value("\${OCR_SPACE_API_KEY:}") private val apiKey: String) : ReceiptOcrClient {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val mapper = JsonMapper.builder().build()

    override fun recognize(bytes: ByteArray, contentType: String): String {
        if (apiKey.isBlank()) throw BusinessException(ErrorCode.OCR_UNAVAILABLE, "영수증 인식이 아직 설정되지 않았습니다. 금액을 직접 입력해 주세요.")
        val fields = mapOf(
            "base64Image" to "data:$contentType;base64,${Base64.getEncoder().encodeToString(bytes)}",
            "OCREngine" to "2", "language" to "auto", "isTable" to "true",
            "isOverlayRequired" to "false", "isCreateSearchablePdf" to "false",
        )
        val body = fields.entries.joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, Charsets.UTF_8)}" }
        val request = HttpRequest.newBuilder(URI.create("https://api.ocr.space/parse/image"))
            .timeout(Duration.ofSeconds(25)).header("apikey", apiKey)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build()
        try {
            // Free endpoint only. No retries or fallback to paid endpoints.
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 429) throw BusinessException(ErrorCode.OCR_LIMIT_REACHED)
            if (response.statusCode() != 200) throw BusinessException(ErrorCode.OCR_UNAVAILABLE)
            return parseResponse(response.body())
        } catch (e: BusinessException) {
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw BusinessException(ErrorCode.OCR_UNAVAILABLE)
        } catch (e: Exception) {
            // Never expose the provider response, receipt contents or credentials.
            throw BusinessException(ErrorCode.OCR_UNAVAILABLE)
        }
    }

    internal fun parseResponse(body: String): String {
        val root = mapper.readTree(body)
        if (root.path("IsErroredOnProcessing").asBoolean(false) || root.path("OCRExitCode").asInt() != 1) {
            throw BusinessException(ErrorCode.OCR_UNAVAILABLE)
        }
        val results = root.path("ParsedResults")
        return (0 until results.size()).map { results.path(it).path("ParsedText").asString() }.joinToString("\n")
            .takeIf { it.isNotBlank() } ?: throw BusinessException(ErrorCode.OCR_UNAVAILABLE)
    }
}
