package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.*;

import java.time.LocalDateTime;

public record ChatRoomResponse(

        Long roomId,
        Long claimId,
        Long postId,
        String postTitle,
        ChatRoomStatus status,
        ClaimStatus claimStatus,
        boolean writer,
        Long partnerId,
        String partnerNickname,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
    public static ChatRoomResponse of(ChatRoom room, Long memberId, ChatMessage lastMessage, long unreadCount) {

        Claim claim = room.getClaim();

        boolean writer = room.getWriter().getId().equals(memberId);

        Member partner = writer ? room.getRequester() : room.getWriter(); // join fetch로 이미 로딩

        return new ChatRoomResponse(
                room.getId(),
                claim == null ? null : claim.getId(),
                claim == null ? null : claim.getPost().getId(),
                room.getPostTitle(),
                room.currentStatus(),
                claim == null ? null : claim.getStatus(),
                writer,
                partner.getId(),
                partner.getNickname(),
                lastMessage == null ? null : lastMessage.getContent(),
                lastMessage == null ? room.getCreatedAt() : lastMessage.getCreatedAt(),
                unreadCount
        );
    }
}
