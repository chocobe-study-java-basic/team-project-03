package com.budzet.standard.ut

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Date

class Ut {
    object jwt {
        @JvmStatic
        fun toString(secret: String, expireMillis: Long, body: Map<String, Any>): String {
            val claimsBuilder = Jwts.claims()
            body.forEach { (key, value) -> claimsBuilder.add(key, value) }
            val claims = claimsBuilder.build()
            val issuedAt = Date()
            val expiration = Date(issuedAt.time + expireMillis)
            val secretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charset.defaultCharset()))
            return Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact()
        }

        @JvmStatic
        fun isValid(jwt: String, secretPattern: String): Boolean {
            val secretKey = Keys.hmacShaKeyFor(secretPattern.toByteArray(StandardCharsets.UTF_8))
            try {
                Jwts.parser().verifyWith(secretKey).build().parse(jwt)
            } catch (e: Exception) {
                return false
            }
            return true
        }

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun payloadOrNull(jwt: String, secretPattern: String): Map<String, Any>? {
            val secretKey = Keys.hmacShaKeyFor(secretPattern.toByteArray(StandardCharsets.UTF_8))
            return if (isValid(jwt, secretPattern)) {
                Jwts.parser().verifyWith(secretKey).build().parse(jwt).payload as Map<String, Any>
            } else {
                null
            }
        }
    }
}
