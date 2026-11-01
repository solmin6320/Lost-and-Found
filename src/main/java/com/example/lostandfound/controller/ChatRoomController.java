package com.example.lostandfound.controller;

import com.example.lostandfound.dto.response.ChatMessageResponse;
import com.example.lostandfound.dto.response.ChatRoomResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // GET /api/chat/rooms
    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(chatRoomService.getMyRooms(userDetails.getMemberId()));
    }

    // GET /api/chat/rooms/{roomId}/messages?before=&size=
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "30") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(chatRoomService.getMessages(roomId, userDetails.getMemberId(), before, size));
    }

    // PATCH /api/chat/rooms/{roomId}/read
    @PatchMapping("/{roomId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        chatRoomService.markRead(roomId, userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}