package com.budzet.domain.invite.dto

import java.time.LocalDateTime

data class InviteResponse(
    val code: String,
    val expireAt: LocalDateTime,
)
