package com.budzet.global.exception

class BusinessException @JvmOverloads constructor(
    val errorCode: ErrorCode,
    message: String? = errorCode.message,
) : RuntimeException(message)
