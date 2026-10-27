package com.budzet.domain.invite.repository

import com.budzet.domain.invite.entity.Invite
import org.springframework.data.jpa.repository.JpaRepository

interface InviteRepository : JpaRepository<Invite, String> {
    fun findAllByRoom_Id(roomId: Long): List<Invite>
}
