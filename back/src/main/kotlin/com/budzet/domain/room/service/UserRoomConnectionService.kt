package com.budzet.domain.room.service

import com.budzet.domain.room.dto.MemberResponse
import com.budzet.domain.room.entity.Authority
import com.budzet.domain.room.entity.UserRoomConnection
import com.budzet.domain.room.entity.UserRoomConnectionId
import com.budzet.domain.room.repository.UserRoomConnectionRepository
import com.budzet.global.exception.BusinessException
import com.budzet.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserRoomConnectionService(
    private val userRoomConnectionRepository: UserRoomConnectionRepository,
) {
    @Transactional(readOnly = true)
    fun getConnection(roomId: Long, actorId: Long, userId: Long): UserRoomConnection {
        validateRoomMember(roomId, actorId)
        return userRoomConnectionRepository.findByUser_IdAndRoom_Id(userId, roomId)
            .orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
    }

    @Transactional(readOnly = true)
    fun getMembers(roomId: Long, actorId: Long): List<MemberResponse> {
        validateRoomMember(roomId, actorId)
        return userRoomConnectionRepository.findAllByRoom_Id(roomId).map(MemberResponse::from)
    }

    private fun validateRoomMember(roomId: Long, actorId: Long) {
        if (!userRoomConnectionRepository.existsById(UserRoomConnectionId(actorId, roomId))) {
            throw BusinessException(ErrorCode.USER_NOT_JOINED_ROOM)
        }
    }

    @Transactional
    fun kickMember(actorId: Long, roomId: Long, targetUserId: Long) {
        val actorConnection = userRoomConnectionRepository.findByUser_IdAndRoom_Id(actorId, roomId)
            .orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
        if (actorConnection.authority != Authority.OWNER && actorConnection.authority != Authority.OPERATOR) {
            throw BusinessException(ErrorCode.ROOM_MANAGER_REQUIRED)
        }
        val targetConnection = userRoomConnectionRepository.findByUser_IdAndRoom_Id(targetUserId, roomId)
            .orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
        if (targetConnection.authority == Authority.OWNER) {
            throw BusinessException(ErrorCode.INVALID_AUTHORITY)
        }
        userRoomConnectionRepository.delete(targetConnection)
    }

    @Transactional
    fun leaveRoom(roomId: Long, userId: Long) {
        val connection = userRoomConnectionRepository.findByUser_IdAndRoom_Id(userId, roomId)
            .orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
        if (connection.authority == Authority.OWNER) {
            throw BusinessException(ErrorCode.CONFLICT)
        }
        userRoomConnectionRepository.delete(connection)
    }
}
