package com.budzet.domain.notification.controller

import com.budzet.domain.notification.service.NotificationService
import com.budzet.global.api.ApiResponse
import com.budzet.global.rq.Rq
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/notifications")
class NotificationController(private val rq: Rq, private val service: NotificationService) {
    @GetMapping
    fun list(@RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) =
        ApiResponse.response(HttpStatus.OK, "내 알림 조회", service.getNotifications(checkNotNull(rq.getActor().id), page, size))

    @GetMapping("/unread-count")
    fun unreadCount() = ApiResponse.response(HttpStatus.OK, "안 읽은 알림 개수", service.getUnreadCount(checkNotNull(rq.getActor().id)))

    @PatchMapping("/{notificationId}/read")
    fun read(@PathVariable notificationId: Long): ResponseEntity<Void> {
        service.markAsRead(checkNotNull(rq.getActor().id), notificationId)
        return ResponseEntity.noContent().build()
    }
}
