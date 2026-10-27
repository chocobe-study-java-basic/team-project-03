package com.budzet.domain.user.dto

import com.budzet.domain.user.entity.User

data class UserLoginResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        @JvmStatic
        fun from(user: User, accessToken: String, refreshToken: String): UserLoginResponse =
            UserLoginResponse(UserDto(user), accessToken, refreshToken)
    }
}
