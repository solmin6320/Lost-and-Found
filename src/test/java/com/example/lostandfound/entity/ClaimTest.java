package com.example.lostandfound.entity;

import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Claim 상태 전이 규칙 단위 테스트
public class ClaimTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long CLAIMANT_ID = 2L;

    @Test
    @DisplayName("요청 직후에는 PENDING이고 글의 확인 질문을 복사해 둠")
    void create_initialState() {

        Post post = post(PostType.FOUND, "안에 든 카드 종류는?");

        Claim claim = claim(post);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(claim.getQuestion()).isEqualTo("안에 든 카드 종류는?");
        assertThat(claim.getDecidedAt()).isNull();
    }

    @Test
    @DisplayName("승인하면 요청은 APPROVED, 글은 연락중")
    void approve() {

        Post post = post(PostType.FOUND, null);
        Claim claim = claim(post);

        claim.approve();

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(claim.getDecidedAt()).isNotNull();
        assertThat(post.getStatus()).isEqualTo(PostStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("이미 연락중인 글의 요청은 승인할 수 없음")
    void approve_postNotOpen() {

        Post post = post(PostType.FOUND, null);
        Claim first = claim(post);
        Claim second = claim(post);

        first.approve();

        assertThatThrownBy(second::approve)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CLAIM_NOT_ACCEPTING);
    }

    @Test
    @DisplayName("대기 중인 요청을 거절하면 REJECTED, 글은 그대로")
    void reject_pending() {
        Post post = post(PostType.FOUND, null);
        Claim claim = claim(post);

        claim.reject();

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(post.getStatus()).isEqualTo(PostStatus.OPEN);
    }

    @Test
    @DisplayName("승인된 요청을 거절하면 승인 취소")
    void reject_approved() {

        Post post = post(PostType.FOUND, null);
        Claim claim = claim(post);
        claim.approve();

        claim.reject();

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(post.getStatus()).isEqualTo(PostStatus.OPEN);
    }

    @Test
    @DisplayName("승인된 요청을 받음")
    void receive() {

        Post post = post(PostType.FOUND, null);
        Claim claim = claim(post);
        claim.approve();

        claim.receive();

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.RECEIVED);
        assertThat(claim.getReceivedAt()).isNotNull();
        assertThat(post.getStatus()).isEqualTo(PostStatus.DONE);
        assertThat(post.isOwnerVerified()).isTrue();
    }

    @Test
    @DisplayName("대기 중인 요청에는 받았어요를 누를 수 없음")
    void receive_pending() {

        Claim claim = claim(post(PostType.FOUND, null));

        assertThatThrownBy(claim::receive)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CLAIM_STATUS);
    }

    @Test
    @DisplayName("거절된 요청은 다시 승인할 수 없음")
    void approve_rejected() {

        Claim claim = claim(post(PostType.FOUND, null));
        claim.reject();

        assertThatThrownBy(claim::approve)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CLAIM_STATUS);
    }

    @Test
    @DisplayName("받는 쪽 - 습득 글은 요청자, 분실 글은 글쓴이")
    void receiverId() {

        Claim onFound = claim(post(PostType.FOUND, null));
        Claim onLost = claim(post(PostType.LOST, null));

        assertThat(onFound.receiverId()).isEqualTo(CLAIMANT_ID);
        assertThat(onLost.receiverId()).isEqualTo(AUTHOR_ID);
    }

    private Member member(Long id, String nickname) {

        Member member = Member.builder()
                .email(nickname + "@test.com")
                .password("encoded")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "id", id);

        return member;
    }

    private Post post(PostType type, String question) {

        return Post.builder()
                .member(member(AUTHOR_ID, "글쓴이"))
                .type(type)
                .title("검정 지갑")
                .content("신흥역 2번 출구 근처")
                .category(PostCategory.WALLET)
                .location("신흥역 2번 출구")
                .lostFoundDate(LocalDate.of(2026, 10, 10))
                .verificationQuestion(question)
                .build();
    }


    private Claim claim(Post post) {

        return Claim.builder()
                .post(post)
                .claimant(member(CLAIMANT_ID, "요청자"))
                .answer("신용카드 2장, 학생증 1장")
                .build();
    }


}
