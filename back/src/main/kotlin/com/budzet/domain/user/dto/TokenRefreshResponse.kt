package com.budzet.domain.user.dto

data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        @JvmStatic
        fun from(accessToken: String, refreshToken: String): TokenRefreshResponse =
            TokenRefreshResponse(accessToken, refreshToken)
    }
}
