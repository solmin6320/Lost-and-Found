package com.example.lostandfound.entity;

import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Post 엔티티 상태 전이 규칙 단위 테스트
public class PostTest {

    @Test
    @DisplayName("등록 직후에는 OPEN 상태이고 조회수는 0")
    void create_initialState() {

        Post post = createPost();

        assertThat(post.getStatus()).isEqualTo(PostStatus.OPEN);
        assertThat(post.getViewCount()).isZero();
        assertThat(post.getUpdatedAt()).isNull(); // 아직 수정된 적 없음
    }

    @Test
    @DisplayName("OPEN 에서 IN_PROGRESS로 변경하면 상태와 수정일시가 갱신")
    void changeStatus_openToInProgress() {

        Post post = createPost();

        post.changeStatus(PostStatus.IN_PROGRESS);

        assertThat(post.getStatus()).isEqualTo(PostStatus.IN_PROGRESS);
        assertThat(post.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("IN_PROGRESS에서 OPEN으로 되돌릴 수 있음(거래 무산 대응)")
    void changeStatus_inProgressToOpen() {

        Post post = createPost();

        post.changeStatus(PostStatus.IN_PROGRESS);

        post.changeStatus(PostStatus.OPEN);

        assertThat(post.getStatus()).isEqualTo(PostStatus.OPEN);
    }

    @Test
    @DisplayName("OPEN에서 DONE으로 바로 갈 수 있음(연락 과정 없이 해결된 경우)")
    void changeStatus_openToDoneDirectly() {

        Post post = createPost();

        post.changeStatus(PostStatus.DONE);

        assertThat(post.getStatus()).isEqualTo(PostStatus.DONE);
    }

    @Test
    @DisplayName("DONE에서 OPEN으로 되돌리면 예외")
    void changeStatus_doneToOpen_throws() {

        Post post = createPost();
        post.changeStatus(PostStatus.DONE);

        assertThatThrownBy(() -> post.changeStatus(PostStatus.OPEN))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("DONE에서 IN_PROGRESS로 되돌려도 예외")
    void changeStatus_doneToInProgress_throws() {
        Post post = createPost();

        post.changeStatus(PostStatus.DONE);

        assertThatThrownBy(() -> post.changeStatus(PostStatus.IN_PROGRESS))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("같은 상태로 바꾸면 수정일시가 갱신되지 않음")
    void changeStatus_sameStatus_doesNothing() {

        Post post = createPost();

        post.changeStatus(PostStatus.OPEN); // 이미 OPEN

        assertThat(post.getStatus()).isEqualTo(PostStatus.OPEN);
        assertThat(post.getUpdatedAt()).isNull(); // 아무 일도 일어나지 않음
    }

    @Test
    @DisplayName("DONE 상태에서 DONE을 다시 보내도 예외가 아님")
    void changeStatus_doneToDone_doesNotThrow() {

        Post post = createPost();
        post.changeStatus(PostStatus.DONE);

        post.changeStatus(PostStatus.DONE); // 예외가 나면 안됨

        assertThat(post.getStatus()).isEqualTo(PostStatus.DONE);
    }





    private Post createPost() {

        Member member = Member.builder()
                .email("test@test.com")
                .password("encoded")
                .nickname("테스터")
                .build();

        return Post.builder()
                .member(member)
                .type(PostType.LOST)
                .title("지갑 잃어버렸어요")
                .content("신흥역 2번 출구 근처입니다")
                .category(PostCategory.WALLET)
                .location("신흥역 2번 출구")
                .lostFoundDate(LocalDate.of(2026, 9, 13))
                .build();
    }
}
