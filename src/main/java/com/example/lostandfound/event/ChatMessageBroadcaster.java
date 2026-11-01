package com.example.lostandfound.event;

import com.example.lostandfound.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String ROOM_TOPIC = "/topic/rooms/";

    // 저장이 커밋된 뒤에만 방에 전달
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void broadcast(ChatMessageSentEvent event) {
        ChatMessageResponse message = event.message();

        try {
            messagingTemplate.convertAndSend(ROOM_TOPIC + message.roomId(), message);
        } catch (RuntimeException e) {
            log.warn("채팅 전달 실패: roomId={}, messageId={}", message.roomId(), message.id(), e);
        }
    }
}
