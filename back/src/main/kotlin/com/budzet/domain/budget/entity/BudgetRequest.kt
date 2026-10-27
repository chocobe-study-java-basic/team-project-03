package com.budzet.domain.budget.entity

import com.budzet.domain.room.entity.Room
import com.budzet.domain.user.entity.User
import com.budzet.global.exception.BusinessException
import com.budzet.global.exception.ErrorCode
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
open class BudgetRequest(room: Room, user: User, reason: String?, requestedAmount: Long?) {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "room_id")
    open var room: Room = room
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "user_id")
    open var user: User = user
        protected set

    @field:Column(length = 50)
    open var userName: String? = user.name
        protected set

    @field:Column(length = 20)
    open var reason: String? = reason
        protected set

    open var requestedAmount: Long? = requestedAmount
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(length = 20)
    open var status: BudgetRequestType = BudgetRequestType.REQUEST
        protected set

    @field:Column(length = 50)
    open var rejectReason: String? = null
        protected set

    @field:CreatedDate
    open var createdAt: LocalDateTime? = null
        protected set

    @field:LastModifiedDate
    open var updatedAt: LocalDateTime? = null
        protected set

    fun changeToSettlement() {
        if (status != BudgetRequestType.APPROVE) {
            throw BusinessException(ErrorCode.BUDGET_REQUEST_NOT_APPROVED)
        }
        status = BudgetRequestType.SETTLEMENT
    }

    fun updateRequest(budgetChange: BudgetChange, user: User) {
        this.user = user
        userName = budgetChange.userName
        reason = budgetChange.reason
        requestedAmount = budgetChange.changeBudget
    }

    fun approveRequest() {
        status = BudgetRequestType.APPROVE
        rejectReason = ""
    }

    fun rejectRequest(rejectReason: String?) {
        status = BudgetRequestType.REJECT
        this.rejectReason = rejectReason
    }

    fun modifyRequest(reason: String?, requestedAmount: Long?) {
        this.reason = reason
        this.requestedAmount = requestedAmount
        status = BudgetRequestType.REQUEST
        rejectReason = ""
    }
}
