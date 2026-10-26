package com.budzet.domain.budget.entity;

import com.budzet.domain.room.entity.Room;
import com.budzet.domain.user.entity.User;
import com.budzet.global.exception.BusinessException;
import com.budzet.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class BudgetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 50)
    private String userName;

    @Column(length = 20)
    private String reason;

    private Long requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BudgetRequestType status;

    @Column(length = 50)
    private String rejectReason;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public BudgetRequest(Room room, User user, String reason, Long requestedAmount){
        this.room = room;
        this.user = user;
        this.userName = user.getName();
        this.reason = reason;
        this.requestedAmount = requestedAmount;
        this.status = BudgetRequestType.REQUEST;
    }

    public void changeToSettlement() {
        if (!BudgetRequestType.APPROVE.equals(this.status)) {
            throw new BusinessException(ErrorCode.BUDGET_REQUEST_NOT_APPROVED);
        }

        this.status = BudgetRequestType.SETTLEMENT;
    }

    public void updateRequest(BudgetChange budgetChange, User user){
        this.user = user;
        this.userName = budgetChange.getUserName();
        this.reason = budgetChange.getReason();
        this.requestedAmount = budgetChange.getChangeBudget();
    }

    public void approveRequest(){
        this.status = BudgetRequestType.APPROVE;
        this.rejectReason = "";
    }

    public void rejectRequest(String rejectReason){
        this.status = BudgetRequestType.REJECT;
        this.rejectReason = rejectReason;
    }

    public void modifyRequest(String reason, Long requestedAmount){
        this.reason = reason;
        this.requestedAmount = requestedAmount;
        this.status = BudgetRequestType.REQUEST;
        this.rejectReason = "";
    }

}
