package com.budzet.domain.receipt

import com.budzet.global.exception.BusinessException
import com.budzet.global.exception.ErrorCode
import org.springframework.web.multipart.MultipartFile
import javax.imageio.ImageIO

object ReceiptImageValidator {
    fun validate(file: MultipartFile): ByteArray {
        if (file.isEmpty || file.size > 1_000_000 || file.contentType !in setOf("image/jpeg", "image/png")) {
            throw BusinessException(ErrorCode.BAD_REQUEST, "1MB 이하의 JPEG 또는 PNG 이미지 한 장을 선택해 주세요.")
        }
        val bytes = file.bytes
        try {
            ImageIO.createImageInputStream(bytes.inputStream()).use { input ->
                val readers = ImageIO.getImageReaders(input)
                if (!readers.hasNext()) throw IllegalArgumentException()
                val reader = readers.next()
                try {
                    reader.input = input
                    val expected = if (file.contentType == "image/png") "png" else "JPEG"
                    require(reader.formatName.equals(expected, ignoreCase = true))
                    require(reader.getWidth(0).toLong() * reader.getHeight(0) <= 12_000_000)
                    require(reader.read(0) != null)
                } finally { reader.dispose() }
            }
        } catch (e: Exception) {
            throw BusinessException(ErrorCode.BAD_REQUEST, "올바른 JPEG·PNG 이미지인지 확인해 주세요. 최대 1,200만 화소까지 지원합니다.")
        }
        return bytes
    }
}