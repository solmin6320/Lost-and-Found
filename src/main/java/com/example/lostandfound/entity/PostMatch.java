package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "POST_MATCH")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_post_id", nullable = false)
    private Post lostPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_post_id", nullable = false)
    private Post foundPost;

    // 정렬용 점수(화면에는 안 보임)
    @Column(nullable = false)
    private int score;

    @Column(name = "dismissed_by_lost", nullable = false)
    private boolean dismissedByLost;

    @Column(name = "dismissed_by_found", nullable = false)
    private boolean dismissedByFound;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private PostMatch(Post lostPost, Post foundPost, int score) {
        this.lostPost = lostPost;
        this.foundPost = foundPost;
        this.score = score;
        this.createdAt = LocalDateTime.now();
    }

    // 내 글 쪽에서 숨김
    public void dismissBy(PostType type) {
        if (type == PostType.LOST) {
            this.dismissedByLost = true;
        } else {
            this.dismissedByFound = true;
        }
    }

    // 글이 수정되면 점수를 다시 매김
    public void changeScore(int score) {
        this.score = score;
    }
}
