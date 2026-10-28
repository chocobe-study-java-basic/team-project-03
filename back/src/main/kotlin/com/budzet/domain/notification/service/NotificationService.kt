package com.budzet.domain.notification.service

import com.budzet.domain.budget.entity.BudgetRequest
import com.budzet.domain.budget.entity.BudgetRequestType
import com.budzet.domain.notification.dto.NotificationListResponse
import com.budzet.domain.notification.dto.NotificationResponse
import com.budzet.domain.notification.dto.UnreadNotificationCountResponse
import com.budzet.domain.notification.entity.Notification
import com.budzet.domain.notification.entity.NotificationType
import com.budzet.domain.notification.repository.NotificationRepository
import com.budzet.domain.room.entity.Authority
import com.budzet.domain.room.entity.Room
import com.budzet.domain.room.entity.UserRoomConnection
import com.budzet.domain.room.entity.UserRoomConnectionId
import com.budzet.domain.room.repository.UserRoomConnectionRepository
import com.budzet.global.exception.BusinessException
import com.budzet.global.exception.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val membershipRepository: UserRoomConnectionRepository,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun notifyBudgetRequested(request: BudgetRequest) {
        notifyManagers(
            room = request.room,
            actorId = checkNotNull(request.user.id),
            type = NotificationType.BUDGET_REQUESTED,
            message = "${request.userName}님이 예산을 신청했습니다: ${request.reason}",
            targetId = checkNotNull(request.id),
        )
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun notifyBudgetDecision(request: BudgetRequest) {
        val (type, message) = when (request.status) {
            BudgetRequestType.APPROVE -> NotificationType.BUDGET_APPROVED to "예산 신청이 승인되었습니다: ${request.reason}"
            BudgetRequestType.REJECT -> NotificationType.BUDGET_REJECTED to "예산 신청이 반려되었습니다: ${request.reason}"
            else -> throw IllegalStateException("승인 또는 반려 처리 후에만 결과 알림을 생성할 수 있습니다.")
        }
        notificationRepository.save(Notification(
            recipient = request.user,
            room = request.room,
            type = type,
            message = message.take(200),
            targetId = checkNotNull(request.id),
        ))
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun notifyMemberJoined(connection: UserRoomConnection) {
        notifyManagers(
            room = connection.room,
            actorId = checkNotNull(connection.user.id),
            type = NotificationType.MEMBER_JOINED,
            message = "${connection.user.name}님이 모임에 참여했습니다.",
            targetId = checkNotNull(connection.room.id),
        )
    }

    private fun notifyManagers(room: Room, actorId: Long, type: NotificationType, message: String, targetId: Long) {
        val notifications = membershipRepository.findAllByRoom_Id(checkNotNull(room.id))
            .filter { it.authority in setOf(Authority.OWNER, Authority.OPERATOR) && it.user.id != actorId }
            .map { Notification(it.user, room, type, message.take(200), targetId) }
        notificationRepository.saveAll(notifications)
    }

    @Transactional(readOnly = true)
    fun getNotifications(userId: Long, page: Int = 0, size: Int = 20): NotificationListResponse {
        if (page !in 0..10_000 || size !in 1..50) {
            throw BusinessException(ErrorCode.BAD_REQUEST, "페이지는 0~10000, 조회 개수는 1~50 범위여야 합니다.")
        }
        val slice = notificationRepository.findVisibleByRecipient(userId, PageRequest.of(page, size))
        return NotificationListResponse(
            notifications = slice.content.map(NotificationResponse::from),
            nextPage = if (slice.hasNext() && page < 10_000) page + 1 else null,
        )
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: Long): UnreadNotificationCountResponse =
        UnreadNotificationCountResponse(notificationRepository.countVisibleUnread(userId))

    @Transactional
    fun markAsRead(userId: Long, notificationId: Long) {
        val notification = notificationRepository.findByIdAndRecipient_Id(notificationId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND)
        if (!membershipRepository.existsById(UserRoomConnectionId(userId, checkNotNull(notification.room.id)))) {
            throw BusinessException(ErrorCode.USER_NOT_JOINED_ROOM)
        }
        notification.markAsRead()
    }
}
