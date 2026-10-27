package com.budzet.domain.budget.service;

import com.budzet.domain.budget.dto.BudgetRequestResponse;
import com.budzet.domain.budget.entity.BudgetRequest;
import com.budzet.domain.budget.entity.BudgetRequestType;
import com.budzet.domain.budget.repository.BudgetRequestRepository;
import com.budzet.domain.room.entity.Authority;
import com.budzet.domain.room.entity.Room;
import com.budzet.domain.room.entity.UserRoomConnection;
import com.budzet.domain.room.entity.UserRoomConnectionId;
import com.budzet.domain.room.repository.RoomRepository;
import com.budzet.domain.room.repository.UserRoomConnectionRepository;
import com.budzet.domain.user.entity.User;
import com.budzet.global.exception.BusinessException;
import com.budzet.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetRequestService {

    private final BudgetRequestRepository budgetRequestRepository;
    private final RoomRepository roomRepository;
    private final UserRoomConnectionRepository userRoomConnectionRepository;

    /**
     * 유저가 전송된 roomId의 방소속인지 확인
     * @param userRoomConnectionId
     * @throws BusinessException USER_NOT_JOINED_ROOM
     */
    private void userInRoomCheck(UserRoomConnectionId userRoomConnectionId){
        if(!userRoomConnectionRepository.existsById(userRoomConnectionId))
            throw new BusinessException(ErrorCode.USER_NOT_JOINED_ROOM);
    }

    /**
     * 예산신청 등록
     * @param roomId
     * @param user
     * @param reason
     * @param requestedAmount
     */
    public void budgetRequestRegistration(Long roomId, User user, String reason, Long requestedAmount){

        userInRoomCheck(new UserRoomConnectionId(user.getId(), roomId));

        Room room = this.roomRepository.findById(roomId).get();
        //신청예산의 가용예산 초과여부 처리
        if(room.getAvailableBudget() < requestedAmount)
            throw new BusinessException(ErrorCode.REQUEST_AMOUNT_OVER_BUDGET);

        budgetRequestRepository.save(new BudgetRequest(room, user, reason, requestedAmount));
    }

    /**
     * 현재방의 예산신청 목록조회
     * @param roomId
     * @param user
     * @return 현재방의 예산신청 목록
     */
    public List<BudgetRequestResponse> getBudgetRequestList(Long roomId, User user){

        userInRoomCheck(new UserRoomConnectionId(user.getId(), roomId));

        return budgetRequestRepository.findDtoByRoomId(roomId);
    }

    /**
     * 예산신청 상세조회
     * @param roomId
     * @param user
     * @param requestId
     * @return requestId의 예산신청 상세정보
     */
    public BudgetRequestResponse getBudgetRequest(Long roomId, User user, Long requestId){

        userInRoomCheck(new UserRoomConnectionId(user.getId(), roomId));

        return budgetRequestRepository.findDtoById(roomId, requestId);
    }

    /**
     * 예산신청 삭제
     * @param roomId
     * @param user
     * @param requestId
     */
    @Transactional
    public void deleteBudgetRequest(Long roomId, User user, Long requestId){

        userInRoomCheck(new UserRoomConnectionId(user.getId(), roomId));

        BudgetRequestResponse budgetRequestResponse= budgetRequestRepository.findDtoById(roomId, requestId);

        Room room = this.roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));


        if(budgetRequestResponse == null)
            throw new BusinessException(ErrorCode.BUDGET_REQUEST_NOT_FOUND);
        else if (!budgetRequestResponse.userId().equals(user.getId()))
            throw new BusinessException(ErrorCode.NOT_BUDGET_REQUESTER);
        else{
            BudgetRequestType status = budgetRequestResponse.status();
            if(BudgetRequestType.APPROVE.equals(status))
                room.updateAvailableBudget(budgetRequestResponse.requestedAmount());
            budgetRequestRepository.deleteById(requestId);
        }
    }

    /**
     * 예산신청 승인
     * @param roomId
     * @param user
     * @param requestId
     */
    @Transactional
    public void approveBudgetRequest(Long roomId, User user, Long requestId){

        userInRoomCheck(new UserRoomConnectionId(user.getId(), roomId));

        // 권한 확인
        UserRoomConnection userRoomConnection = userRoomConnectionRepository.findByUser_IdAndRoom_Id(user.getId(), roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_JOINED_ROOM));
        if(userRoomConnection.getAuthority() == Authority.MEMBER)
            throw new BusinessException(ErrorCode.OWNER_REQUIRED);

        Room room = roomRepository.findByWithLock(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        BudgetRequest budgetRequest = budgetRequestRepository.findByIdAndRoomId(requestId, roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUDGET_REQUEST_NOT_FOUND));

        BudgetRequestType status = budgetRequest.getStatus();
        if(!BudgetRequestType.REQUEST.equals(status))
            throw new BusinessException(ErrorCode.NOT_APPROVABLE);
        else if(room.getAvailableBudget() < budgetRequest.getRequestedAmount())
            throw new BusinessException(ErrorCode.BUDGET_EXCEEDED);
        else{
            room.updateAvailableBudget(-1 * budgetRequest.getRequestedAmount());
            budgetRequest.approveRequest();
        }
    }

    /**
     * 예산신청 반려
     * @param roomId
     * @param user
     * @param requestId
     */
    @Transactional
    public void rejectBudgetRequest(Long roomId, User user, Long requestId, String rejectReason){

        userInRoomCheck(new UserRoomConnectionId(user.getId(), roomId));

        // 권한 확인
        UserRoomConnection userRoomConnection =  userRoomConnectionRepository.findByUser_IdAndRoom_Id(user.getId(), roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_JOINED_ROOM));
        if(userRoomConnection.getAuthority() == Authority.MEMBER)
            throw new BusinessException(ErrorCode.OWNER_REQUIRED);

        BudgetRequest budgetRequest = budgetRequestRepository.findByIdAndRoomId(requestId, roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUDGET_REQUEST_NOT_FOUND));

        BudgetRequestType status = budgetRequest.getStatus();
        if(!BudgetRequestType.REQUEST.equals(status))
            throw new BusinessException(ErrorCode.NOT_REJECTABLE);
        else
            budgetRequest.rejectRequest(rejectReason);
    }

    /**
     * 예산신청 수정
     * @param roomId
     * @param user
     * @param requestId
     * @param reason
     * @param requestedAmount
     */
    @Transactional
    public void modifyBudgetRequest(Long roomId, User user, Long requestId, String reason, Long requestedAmount){

        userInRoomCheck(new UserRoomConnectionId(user.getId(), roomId));


        Room room = this.roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        BudgetRequest budgetRequest = budgetRequestRepository.findByIdAndRoomId(requestId, roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUDGET_REQUEST_NOT_FOUND));

        BudgetRequestType status = budgetRequest.getStatus();
        if(BudgetRequestType.SETTLEMENT.equals(status))
            throw new BusinessException(ErrorCode.NOT_MODIFYABLE);
        else if(room.getAvailableBudget() + budgetRequest.getRequestedAmount() < requestedAmount)
            throw new BusinessException(ErrorCode.REQUEST_AMOUNT_OVER_BUDGET);
        else if (!budgetRequest.getUser().getId().equals(user.getId()))
            throw new BusinessException(ErrorCode.NOT_BUDGET_REQUESTER);
        else{
            if(budgetRequest.getStatus().equals(BudgetRequestType.APPROVE))
                room.updateAvailableBudget(budgetRequest.getRequestedAmount() - requestedAmount);
            budgetRequest.modifyRequest(reason, requestedAmount);
        }
    }

}
