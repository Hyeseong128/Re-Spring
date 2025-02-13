package org.ssafy.respring.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.ssafy.respring.domain.notification.dto.NotificationDto;
import org.ssafy.respring.domain.notification.service.NotificationService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "알림 API", description = "SSE 방식의 실시간 알림 기능을 제공합니다.")
public class NotificationController {

    private final NotificationService notificationService;
    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    private UUID requireLogin(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("❌ 로그인이 필요합니다.");
        }
        return userId;
    }

    @GetMapping
    @Operation(summary = "전체 알림 조회", description = "사용자가 받은 알림을 조회합니다.")
    public ResponseEntity<List<NotificationDto>> getNotifications(HttpSession session) {
        UUID userId = requireLogin(session);
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    // ✅ 특정 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "특정 알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable Long notificationId,
            HttpSession session
    ) {
        UUID userId = requireLogin(session);
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok().build();
    }

    // ✅ 모든 알림 읽음 처리
    @PatchMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리", description = "모든 알림을 동시에 읽음 처리합니다.")
    public ResponseEntity<Void> markAllNotificationsAsRead(HttpSession session) {
        UUID userId = requireLogin(session);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }




}
