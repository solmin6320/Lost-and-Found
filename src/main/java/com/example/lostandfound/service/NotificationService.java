package com.example.lostandfound.service;

import com.example.lostandfound.dto.response.NotificationResponse;
import com.example.lostandfound.entity.Notification;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 내 알림
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotification(Long memberId, Pageable pageable) {
        return notificationRepository.findByReceiverIdOrderByIdDesc(memberId, pageable)
                .map(NotificationResponse::from);
    }

    // 안 읽은 알림 수
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public long countUnread(Long memberId) {
        return notificationRepository.countByReceiverIdAndReadAtIsNull(memberId);
    }

    // 하나 읽음
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void markRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n ->n.getReceiver().getId().equals(memberId))
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markRead();
    }

    // 모두 읽음
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void markAllRead(Long memberId) {

        notificationRepository.markAllRead(memberId, LocalDateTime.now());
    }
}
