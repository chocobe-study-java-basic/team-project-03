package com.budzet.domain.user.dto

import com.budzet.domain.user.entity.User

data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
) {
    constructor(user: User) : this(
        id = user.id,
        email = user.email,
        name = user.name,
    )
}
