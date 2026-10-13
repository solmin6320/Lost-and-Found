package com.example.lostandfound.entity;

import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "CLAIM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "claim_id")
    private Long id;

    // 대상 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 요청한 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimant_id", nullable = false)
    private Member claimant;

    // 요청 시점의 확인 질문
    @Column(length = 200)
    private String question;

    // 비공개 답
    @Column(nullable = false, length = 500)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 승인 또는 거절 시각
    private LocalDateTime decidedAt;

    // 주인이 받은 시각
    private LocalDateTime receivedAt;

    @Builder
    private Claim(Post post, Member claimant, String answer) {
        this.post = post;
        this.claimant = claimant;
        this.question = post.getVerificationQuestion();
        this.answer = answer;
        this.status = ClaimStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // 승인
    public void approve() {
        if (this.status != ClaimStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_CLAIM_STATUS);
        }
        this.post.startHandover();
        this.status = ClaimStatus.APPROVED;
        this.decidedAt = LocalDateTime.now();
    }

    // 거절
    public void reject() {
        if (this.status == ClaimStatus.APPROVED) {
            this.post.changeStatus(PostStatus.OPEN);
        } else if (this.status != ClaimStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_CLAIM_STATUS);
        }

        this.status = ClaimStatus.REJECTED;
        this.decidedAt = LocalDateTime.now();
    }

    // 받은 상황(승인된 요청만)
    public void receive() {
        if (this.status != ClaimStatus.APPROVED) {
            throw new CustomException(ErrorCode.INVALID_CLAIM_STATUS);
        }

        this.post.completeByOwner();
        this.status = ClaimStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }

    // 물건을 받는 쪽(주인)
    public Long receiverId() {
        return this.post.getType() == PostType.FOUND ? this.claimant.getId()
                : this.post.getMember().getId();
    }
}
