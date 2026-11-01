package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.ChatMessage;
import com.example.lostandfound.entity.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(

        Long id,
        Long roomId,
        Long senderId, // 시스템 메시지는 null
        ChatMessageType type,
        String content, // 가린 뒤 본문
        boolean masked,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getSender() == null ? null : message.getSender().getId(),
                message.getType(),
                message.getContent(),
                message.isMasked(),
                message.getCreatedAt()
        );
    }
}
