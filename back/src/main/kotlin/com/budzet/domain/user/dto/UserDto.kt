package com.budzet.domain.user.dto

import com.budzet.domain.user.entity.User

data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
) {
    constructor(user: User) : this(
        id = checkNotNull(user.id) {
            "저장된 회원만 UserDto로 변환할 수 있습니다."
        },
        email = user.email,
        name = user.name,
    )
}
