package com.budzet.domain.user.service

import com.budzet.domain.user.entity.User
import com.budzet.standard.ut.Ut
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthTokenService {
    @Value("\${custom.jwt.access-secret}")
    private lateinit var accessSecret: String

    @Value("\${custom.jwt.access-expire-millis}")
    private var accessExpireMillis: Long = 0

    @Value("\${custom.jwt.refresh-secret}")
    private lateinit var refreshSecret: String

    @Value("\${custom.jwt.refresh-expire-millis}")
    private var refreshExpireMillis: Long = 0

    fun genAccessToken(user: User): String =
        Ut.jwt.toString(accessSecret, accessExpireMillis, mapOf("id" to requireNotNull(user.id)))

    fun genRefreshToken(user: User): String =
        Ut.jwt.toString(refreshSecret, refreshExpireMillis, mapOf("id" to requireNotNull(user.id)))

    fun accessPayloadOrNull(accessToken: String): Map<String, Any>? = payloadOrNull(accessToken, accessSecret)

    fun refreshPayloadOrNull(refreshToken: String): Map<String, Any>? = payloadOrNull(refreshToken, refreshSecret)

    private fun payloadOrNull(jwt: String, secret: String): Map<String, Any>? {
        val payload = Ut.jwt.payloadOrNull(jwt, secret) ?: return null
        val id = (payload["id"] as Number).toLong()
        return mapOf("id" to id)
    }
}
