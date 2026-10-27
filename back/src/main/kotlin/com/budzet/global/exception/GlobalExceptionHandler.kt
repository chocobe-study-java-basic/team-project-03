package com.budzet.global.exception

import com.budzet.global.api.ApiResponse
import jakarta.persistence.LockTimeoutException
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    // 비즈니스 로직 에러 처리
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ApiResponse<Void>> {
        val errorCode = exception.errorCode
        val apiResponse = ApiResponse<Void>(
            resultCode = errorCode.status.value(),
            message = exception.message,
            data = null,
        )

        return ResponseEntity.status(errorCode.status).body(apiResponse)
    }

    // @Valid 에러 처리
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        exception: MethodArgumentNotValidException,
    ): ResponseEntity<ApiResponse<List<FieldErrorResponse>>> {
        val fieldErrors = exception.bindingResult.fieldErrors.map(FieldErrorResponse::from)

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorCode.BAD_REQUEST, fieldErrors))
    }

    // TODO:
    // 이 에러 정의 확인 필요
    // 언제 발생되는지 확인 필요
    // 응답은 어떤 값인지 예시 확인 필요
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        exception: ConstraintViolationException,
    ): ResponseEntity<ApiResponse<List<FieldErrorResponse>>> {
        val fieldErrors = exception.constraintViolations.map {
            FieldErrorResponse(it.propertyPath.toString(), it.message)
        }

        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST, fieldErrors))
    }

    // 요청 body 불일치 에러 처리
    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleInvalidRequest(exception: Exception): ResponseEntity<ApiResponse<Void>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ErrorCode.BAD_REQUEST))

    @ExceptionHandler(
        PessimisticLockingFailureException::class,
        LockTimeoutException::class,
    )
    fun handleLockTimeoutException(exception: Exception): ResponseEntity<ApiResponse<Void>> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.error(ErrorCode.LOCKED_RESOURCE))

    // 그 외 에러는 `500`으로 응답 (기존처럼 비활성 상태 유지)
    // @ExceptionHandler(Exception::class)
    // fun handleException(exception: Exception): ResponseEntity<ApiResponse<Void>> =
    //     ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR))
}
