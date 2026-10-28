package com.budzet.global.security

import com.budzet.domain.user.service.UserService
import com.budzet.global.exception.BusinessException
import com.budzet.global.exception.ErrorCode
import com.budzet.global.rq.Rq
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomAuthenticationFilter(
    private val rq: Rq,
    private val userService: UserService,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        // 수집 경로는 전용 SecurityFilterChain에서 Basic 인증으로 처리한다.
        if (path == "/actuator/prometheus" || path.startsWith("/actuator/prometheus/")) {
            return true
        }
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path == "/swagger-ui.html") {
            return true
        }
        return path in listOf("/users/join", "/users/login", "/users/refresh")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        logger.debug("CustomAuthenticationFilter called")
        try {
            authenticate(request, response, filterChain)
        } catch (e: BusinessException) {
            val errorCode = e.errorCode
            response.contentType = "application/json; charset=UTF-8"
            response.status = errorCode.status.value()
            response.writer.write(
                """
                {
                    "resultCode": "%s",
                    "errorCode": "%s",
                    "message": "%s"
                }
                """.trimIndent().format(errorCode.status.value(), errorCode.name, errorCode.message),
            )
        }
    }

    private fun authenticate(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val headerAuthorization = rq.getHeader("Authorization", "")
        val accessToken = if (headerAuthorization.isNotBlank()) {
            if (!headerAuthorization.startsWith("Bearer ")) {
                throw BusinessException(ErrorCode.INVALID_AUTHORIZATION_HEADER)
            }
            headerAuthorization.substring(7)
        } else {
            rq.getCookieValue("accessToken", "")
        }
        if (accessToken.isBlank()) {
            filterChain.doFilter(request, response)
            return
        }
        val payload = userService.accessPayloadOrNull(accessToken)
            ?: throw BusinessException(ErrorCode.INVALID_ACCESS_TOKEN)
        val id = (payload["id"] as Number).toLong()
        val user = userService.findByIdOrNull(id) ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        val securityUser = SecurityUser(user)
        val authentication = UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
        SecurityContextHolder.getContext().authentication = authentication
        filterChain.doFilter(request, response)
    }
}
