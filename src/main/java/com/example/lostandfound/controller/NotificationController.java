package com.example.lostandfound.controller;

import com.example.lostandfound.dto.response.NotificationResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.NotificationService;
import com.example.lostandfound.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.awt.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;

    // GET /api/notifications/stream
    // 실시간 알림 연결(SSE)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        return notificationStreamService.subscribe(userDetails.getMemberId(), lastEventId);
    }

    // GET /api/notifications?page=&size=
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        return ResponseEntity.ok(notificationService.getMyNotification(userDetails.getMemberId(), pageable));
    }

    // GET /api/notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userDetails.getMemberId())));
    }

    // PATCH /api/notifications/{id}/read
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        notificationService.markRead(userDetails.getMemberId(), id);

        return ResponseEntity.noContent().build();
    }

    // PATCH /api/notifications/read-all
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        notificationService.markAllRead(userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
