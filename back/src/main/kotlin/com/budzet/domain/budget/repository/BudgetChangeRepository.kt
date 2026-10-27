package com.budzet.domain.budget.repository

import com.budzet.domain.budget.entity.BudgetChange
import com.budzet.domain.budget.entity.BudgetType
import org.springframework.data.jpa.repository.JpaRepository

interface BudgetChangeRepository : JpaRepository<BudgetChange, Long> {
    fun findAllByRoomIdOrderByCreatedAtDesc(roomId: Long): List<BudgetChange>

    fun findAllByRoomIdAndTypeOrderByCreatedAtDesc(roomId: Long, type: BudgetType): List<BudgetChange>

    fun findAllByRoomIdAndTypeInOrderByCreatedAtDesc(roomId: Long, budgetTypes: Collection<BudgetType>): List<BudgetChange>
}
