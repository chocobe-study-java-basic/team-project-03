package com.budzet.domain.room.service;

import com.budzet.domain.room.entity.Authority;
import com.budzet.domain.room.entity.UserRoomConnection;
import com.budzet.domain.room.entity.UserRoomConnectionId;
import com.budzet.domain.room.repository.UserRoomConnectionRepository;
import com.budzet.global.exception.BusinessException;
import com.budzet.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.budzet.domain.room.dto.MemberResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRoomConnectionService {

    private final UserRoomConnectionRepository userRoomConnectionRepository;

    @Transactional(readOnly = true)
    public UserRoomConnection getConnection(Long roomId, Long actorId, Long userId) {
        validateRoomMember(roomId, actorId);

        return userRoomConnectionRepository
                .findByUser_IdAndRoom_Id(userId, roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(Long roomId, Long actorId) {
        validateRoomMember(roomId, actorId);
        return userRoomConnectionRepository.findAllByRoom_Id(roomId)
                .stream()
                .map(MemberResponse::from)
                .toList();
    }

    private void validateRoomMember(Long roomId, Long actorId) {
        if (!userRoomConnectionRepository.existsById(new UserRoomConnectionId(actorId, roomId))) {
            throw new BusinessException(ErrorCode.USER_NOT_JOINED_ROOM);
        }
    }

    @Transactional
    public void kickMember(
            Long actorId,
            Long roomId,
            Long targetUserId
    ) {
        UserRoomConnection actorConnection =
                userRoomConnectionRepository
                        .findByUser_IdAndRoom_Id(actorId, roomId)
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
                        );

        if (actorConnection.getAuthority() != Authority.OWNER
                && actorConnection.getAuthority() != Authority.OPERATOR) {
            throw new BusinessException(ErrorCode.ROOM_MANAGER_REQUIRED);
        }

        UserRoomConnection targetConnection =
                userRoomConnectionRepository
                        .findByUser_IdAndRoom_Id(targetUserId, roomId)
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
                        );

        if (targetConnection.getAuthority() == Authority.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_AUTHORITY);
        }

        userRoomConnectionRepository.delete(targetConnection);
    }

    @Transactional
    public void leaveRoom(Long roomId, Long userId) {

        UserRoomConnection connection =
                userRoomConnectionRepository
                        .findByUser_IdAndRoom_Id(userId, roomId)
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
                        );

        if(connection.getAuthority() == Authority.OWNER) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        userRoomConnectionRepository.delete(connection);
    }
}