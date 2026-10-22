package com.example.lostandfound.event;

import com.example.lostandfound.entity.NotificationType;

// 알림을 만들 일
public record NotificationEvent(
        Long receiveId,
        NotificationType type,
        Long postId,
        Long claimId,
        String postTitle,
        String actorNickname
) {
}
