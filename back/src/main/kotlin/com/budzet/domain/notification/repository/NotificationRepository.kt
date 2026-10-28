package com.budzet.domain.notification.repository

import com.budzet.domain.notification.entity.Notification
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository : JpaRepository<Notification, Long> {
    @Query("""
        select n from Notification n
        join fetch n.room
        where n.recipient.id = :userId
          and exists (
              select 1 from UserRoomConnection membership
              where membership.user.id = :userId
                and membership.room = n.room
          )
        order by n.id desc
    """)
    fun findVisibleByRecipient(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Slice<Notification>

    @Query("""
        select count(n) from Notification n
        where n.recipient.id = :userId
          and n.readAt is null
          and exists (
              select 1 from UserRoomConnection membership
              where membership.user.id = :userId
                and membership.room = n.room
          )
    """)
    fun countVisibleUnread(@Param("userId") userId: Long): Long

    fun findByIdAndRecipient_Id(id: Long, userId: Long): Notification?
}
