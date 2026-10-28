package com.budzet.domain.budget.repository

import com.budzet.domain.budget.dto.BudgetRequestResponse
import com.budzet.domain.budget.entity.BudgetRequest
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface BudgetRequestRepository : JpaRepository<BudgetRequest, Long> {
    @Query("""
        select new com.budzet.domain.budget.dto.BudgetRequestResponse(
            br.id, br.room.id, br.user.id, br.userName, br.reason, br.requestedAmount,
            br.status, br.rejectReason, br.createdAt, br.updatedAt)
        from BudgetRequest br
        where br.room.id = :roomId
    """)
    fun findDtoByRoomId(@Param("roomId") roomId: Long): List<BudgetRequestResponse>

    @Query("""
        select new com.budzet.domain.budget.dto.BudgetRequestResponse(
            br.id, br.room.id, br.user.id, br.userName, br.reason, br.requestedAmount,
            br.status, br.rejectReason, br.createdAt, br.updatedAt)
        from BudgetRequest br
        where br.room.id = :roomId and br.id = :requestId
    """)
    fun findDtoById(@Param("roomId") roomId: Long, @Param("requestId") requestId: Long): BudgetRequestResponse?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByIdAndRoomId(id: Long, roomId: Long): BudgetRequest?
}
