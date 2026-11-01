package com.example.lostandfound.event;

import com.example.lostandfound.dto.response.ChatMessageResponse;

// 채팅 메시지가 저장됨
public record ChatMessageSentEvent(
    ChatMessageResponse message
) {
}
