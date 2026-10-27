package com.budzet.global.api

import com.budzet.global.exception.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

data class ApiResponse<T>(
    val resultCode: Int,
    val message: String?,
    val data: T?,
) {
    companion object {
        @JvmStatic
        fun <T> success(status: HttpStatus, message: String?, data: T?): ApiResponse<T> =
            ApiResponse(status.value(), message, data)

        @JvmStatic
        fun error(errorCode: ErrorCode): ApiResponse<Void> =
            ApiResponse(errorCode.status.value(), errorCode.message, null)

        @JvmStatic
        fun <T> error(errorCode: ErrorCode, data: T?): ApiResponse<T> =
            ApiResponse(errorCode.status.value(), errorCode.message, data)

        @JvmStatic
        fun <T> response(status: HttpStatus, message: String?, data: T?): ResponseEntity<ApiResponse<T>> =
            ResponseEntity.status(status).body(success(status, message, data))
    }
}
