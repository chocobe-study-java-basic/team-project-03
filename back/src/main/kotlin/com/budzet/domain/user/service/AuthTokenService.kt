package com.budzet.domain.user.service

import com.budzet.domain.user.entity.User
import com.budzet.standard.ut.Ut
import com.budzet.standard.ut.Ut.jwt.payloadOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthTokenService(
    @Value("\${custom.jwt.access-secret}")
    private val accessSecret: String,

    @Value("\${custom.jwt.access-expire-millis}")
    private val accessExpireMillis: Long,

    @Value("\${custom.jwt.refresh-secret}")
    private val refreshSecret: String,

    @Value("\${custom.jwt.refresh-expire-millis}")
    private val refreshExpireMillis: Long,
) {
    fun genAccessToken(user: User): String =
        Ut.jwt.toString(
            accessSecret,
            accessExpireMillis,
            mapOf("id" to user.id),
        )

    fun genRefreshToken(user: User): String =
        Ut.jwt.toString(
            refreshSecret,
            refreshExpireMillis,
            mapOf("id" to user.id),
        )

    fun accessPayloadOrNull(accessToken: String): Map<String, Any>? =
        payloadOrNull(accessToken, accessSecret)

    fun refreshPayloadOrNull(refreshToken: String): Map<String, Any>? =
        payloadOrNull(refreshToken, refreshSecret)

    private fun payloadOrNull(jwt: String, secret: String): Map<String, Any>? {
        val payload = Ut.jwt.payloadOrNull(jwt, secret) ?: return null
        val id = (payload["id"] as? Number)?.toLong() ?: return null

        return mapOf("id" to id)
    }
}
