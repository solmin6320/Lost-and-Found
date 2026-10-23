package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Notification;
import com.example.lostandfound.entity.NotificationType;

import java.time.LocalDateTime;

// 알림 응답
public record NotificationResponse(

        Long id,
        NotificationType type,
        Long postId,
        Long claimId,
        String postTitle,
        String actorNickname,
        boolean read,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getPostId(),
                notification.getClaimId(),
                notification.getPostTitle(),
                notification.getActorNickname(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
