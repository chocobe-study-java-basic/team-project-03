package com.budzet.global.exception

import org.springframework.validation.FieldError

data class FieldErrorResponse(
    val field: String,
    val message: String?,
) {
    companion object {
        @JvmStatic
        fun from(fieldError: FieldError): FieldErrorResponse = FieldErrorResponse(
            field = fieldError.field,
            message = fieldError.defaultMessage,
        )
    }
}
