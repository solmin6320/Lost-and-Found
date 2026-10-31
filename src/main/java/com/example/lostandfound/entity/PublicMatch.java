package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "PUBLIC_MATCH")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "public_match_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_post_id", nullable = false)
    private Post lostPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_item_id", nullable = false)
    private PublicFoundItem publicItem;

    // 정렬용 점수
    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean dismissed;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private PublicMatch(Post lostPost, PublicFoundItem publicItem, int score) {
        this.lostPost = lostPost;
        this.publicItem = publicItem;
        this.score = score;
        this.createdAt = LocalDateTime.now();
    }

    public void dismiss() {
        this.dismissed = true;
    }
}
