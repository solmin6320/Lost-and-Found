package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_MESSAGE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    // 시스템 메시지는 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatMessageType type;

    // 개인정보를 가린 뒤의 본문
    @Column(nullable = false, length = 1000)
    private String content;

    // 가린 곳이 있는지
    @Column(nullable = false)
    private boolean masked;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ChatMessage(ChatRoom room, Member sender, ChatMessageType type, String content, boolean masked) {
        this.room = room;
        this.sender = sender;
        this.type = type;
        this.content = content;
        this.masked = masked;
        this.createdAt = LocalDateTime.now();
    }
}