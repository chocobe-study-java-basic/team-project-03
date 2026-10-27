package com.budzet.controller

import com.budzet.global.api.ApiResponse
import com.budzet.global.exception.BusinessException
import com.budzet.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
class HelloController {
    @GetMapping("/api/hello")
    fun hello(): ResponseEntity<ApiResponse<String>> {
        val apiResponse = ApiResponse.success(HttpStatus.CREATED, "데이터를 생성하였습니다.", "Hello, Budzet!")
        return ResponseEntity.status(apiResponse.resultCode).body(apiResponse)
    }

    @GetMapping("/api/exception")
    fun exception(): ApiResponse<Void> {
        throw BusinessException(ErrorCode.NOT_FOUND, "에러 발생 테스트중입니다.")
    }
}
