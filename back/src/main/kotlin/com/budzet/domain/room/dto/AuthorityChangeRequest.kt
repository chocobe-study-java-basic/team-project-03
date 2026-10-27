package com.budzet.domain.room.dto

import com.budzet.domain.room.entity.Authority
import jakarta.validation.constraints.NotNull

data class AuthorityChangeRequest(
    @field:NotNull
    val authority: Authority? = null,
)
