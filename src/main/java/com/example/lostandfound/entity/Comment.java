package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 게시글 댓글
@Entity
@Table(name = "COMMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    // Post에 @OneToMany를 두지 않는 단방향
    // 이미지와 댓글을 동시에 fetch join하면 예외가 발생함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 작성자(소유자 검증에 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // updatedAt은 추후 추가할 예정(V2 적용 후)

    @Builder
    private Comment(Post post, Member member, String content) {
        this.post = post;
        this.member = member;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
