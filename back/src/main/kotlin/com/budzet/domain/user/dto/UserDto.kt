package com.budzet.domain.user.dto

import com.budzet.domain.user.entity.User

data class UserDto(
    val id: Long?,
    val email: String,
    val name: String,
) {
    constructor(user: User) : this(user.id, user.email, user.name)
}
