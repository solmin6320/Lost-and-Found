package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "CHAT_ROOM")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    // 방을 만든 주인 확인 요청
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", unique = true)
    private Claim claim;

    // 글쓴이
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private Member writer;

    // 요청한 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Member requester;

    // 방이 생길 때 복사한 글 제목
    @Column(name = "post_title", nullable = false, length = 100)
    private String postTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatRoomStatus status;

    // 차단한 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by")
    private Member blockedBy;

    // 마지막으로 읽은 메시지 ID - 안 읽은 수 계산
    @Column(name = "writer_last_read_id")
    private Long writerLastReadId;

    @Column(name = "requester_last_read_id")
    private Long requesterLastReadId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;


    // 승인된 요청으로 방을 만듦
    @Builder
    private ChatRoom(Claim claim) {
        this.claim = claim;
        this.writer = claim.getPost().getMember();
        this.requester = claim.getClaimant();
        this.postTitle = claim.getPost().getTitle();
        this.status = ChatRoomStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        }
}
