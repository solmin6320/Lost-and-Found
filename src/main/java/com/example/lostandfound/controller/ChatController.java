package com.example.lostandfound.controller;

import com.example.lostandfound.dto.request.ChatSendRequest;
import com.example.lostandfound.dto.response.ChatMessageResponse;
import com.example.lostandfound.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ROOM_TOPIC = "/topic/rooms/";

    @MessageMapping("/rooms/{roomId}")
    public void send(@DestinationVariable Long roomId,
                     @Valid @Payload ChatSendRequest request,
                     Principal principal) {

        Long memberId = Long.valueOf(principal.getName()); // 연결 때 넣은 사용자

        ChatMessageResponse response = chatRoomService.send(roomId, memberId, request.content());

        messagingTemplate.convertAndSend(ROOM_TOPIC + roomId, response);
    }
}
