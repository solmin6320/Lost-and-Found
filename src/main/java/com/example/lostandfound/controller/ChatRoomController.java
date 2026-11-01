package com.example.lostandfound.controller;

import com.example.lostandfound.dto.response.ChatRoomResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
