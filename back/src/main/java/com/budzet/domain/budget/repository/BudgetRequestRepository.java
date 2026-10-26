package com.budzet.domain.budget.repository;

import com.budzet.domain.budget.dto.BudgetRequestResponse;
import com.budzet.domain.budget.entity.BudgetRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRequestRepository extends JpaRepository<BudgetRequest, Long> {

    /**
     * roomId가 일치하는 예산신청 목록반환, roomId, userId 직접반환처리
     * @param roomId
     * @return 예산신청 목록
     */
    @Query("select new com.budzet.domain.budget.dto.BudgetRequestResponse(" +
            "br.id, br.room.id, br.user.id, br.userName, br.reason, br.requestedAmount, br.status, br.rejectReason, br.createdAt, br.updatedAt) " +
            "from BudgetRequest br " +
            "where br.room.id = :roomId")
    List<BudgetRequestResponse> findDtoByRoomId(@Param("roomId") Long roomId);

    /**
     * roomId와 userId가 일치하는 예산신청 단일항목반환, roomId, userId 직접반환처리
     * @param roomId
     * @param requestId
     * @return 예산신청 단일항목
     */
    @Query("select new com.budzet.domain.budget.dto.BudgetRequestResponse(" +
            "br.id, br.room.id, br.user.id, br.userName, br.reason, br.requestedAmount, br.status, br.rejectReason, br.createdAt, br.updatedAt) " +
            "from BudgetRequest br " +
            "where br.room.id = :roomId and br.id = :requestId")
    BudgetRequestResponse findDtoById(@Param("roomId") Long roomId, @Param("requestId") Long requestId);

    /**
     * id, room_id, user_id 가 일치하는 예산신청 조회
     * @param id
     * @param roomId
     * @return 예산신청 단일항목
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BudgetRequest> findByIdAndRoomId(Long id, Long roomId);

}
