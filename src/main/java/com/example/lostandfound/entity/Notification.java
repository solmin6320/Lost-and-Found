package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    // 받는 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // 대상 글, 요청
    private Long postId;

    private Long claimId;

    // 문장에 쓸 값
    @Column(length = 100)
    private String postTitle;

    @Column(length = 20)
    private String actorNickname;

    // 읽은 시각(null 이면 안 읽음)
    private LocalDateTime readAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Notification(Member receiver, NotificationType type, Long postId, Long claimId, String postTitle, String actorNickname) {
        this.receiver = receiver;
        this.type = type;
        this.postId = postId;
        this.claimId = claimId;
        this.postTitle = postTitle;
        this.actorNickname = actorNickname;
        this.createdAt = LocalDateTime.now();
    }

    // 읽음 처리
    public void markRead() {

        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isRead() {
        return readAt != null;
    }
}
