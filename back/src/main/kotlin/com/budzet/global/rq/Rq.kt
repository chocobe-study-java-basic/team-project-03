package com.budzet.global.rq

import com.budzet.domain.user.entity.User
import com.budzet.global.security.SecurityUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class Rq(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
) {
    fun getActor(): User {
        val authentication = requireNotNull(SecurityContextHolder.getContext().authentication)
        return (authentication.principal as SecurityUser).user
    }

    fun getCookieValue(name: String, defaultValue: String): String =
        request.cookies?.firstOrNull { it.name == name && it.value.isNotBlank() }?.value ?: defaultValue

    fun addCookie(name: String, value: String, maxAge: Duration) {
        val cookie = ResponseCookie.from(name, value)
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .maxAge(maxAge)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    fun deleteCookie(name: String) {
        val cookie = ResponseCookie.from(name, "")
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .maxAge(0)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    fun getHeader(name: String, defaultValue: String): String =
        request.getHeader(name)?.takeIf { it.isNotBlank() } ?: defaultValue
}
