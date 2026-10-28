package com.budzet.domain.receipt

import com.budzet.domain.budget.entity.BudgetChange
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.springframework.data.jpa.repository.JpaRepository

@Entity
open class ReceiptEvidence(
    @field:OneToOne(fetch = FetchType.LAZY, optional = false)
    @field:JoinColumn(name = "change_id", nullable = false, unique = true)
    @field:OnDelete(action = OnDeleteAction.CASCADE)
    open var change: BudgetChange,
    @field:Column(nullable = false, length = 20)
    open var contentType: String,
    @field:Lob
    @field:Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    open var image: ByteArray,
) {
    @field:Id @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set
}

interface ReceiptEvidenceRepository : JpaRepository<ReceiptEvidence, Long> {
    fun findByChange_Id(changeId: Long): ReceiptEvidence?
}
