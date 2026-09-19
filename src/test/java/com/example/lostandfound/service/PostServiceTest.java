package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.PostCreateRequest;
import com.example.lostandfound.dto.response.PostDetailResponse;
import com.example.lostandfound.dto.response.PostResponse;
import com.example.lostandfound.dto.response.PostStatusResponse;
import com.example.lostandfound.entity.*;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.CommentRepository;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import com.example.lostandfound.config.AwsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// PostService 단위 테스트
@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostViewService postViewService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private S3Service s3Service;

    // 설정값은 목이 아니라 실제 객체
    private final AwsProperties awsProperties = new AwsProperties(
            "ap-northeast-2",
            new AwsProperties.S3("test-bucket", "https://cdn.test"),
            new AwsProperties.Credentials("test-key", "test-secret")
    );

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, memberRepository, postViewService, commentRepository, s3Service, awsProperties);
    }

    @Test
    @DisplayName("작성자 본인이면 게시글 상태를 변경")
    void changeStatus_owner_success() {

        Post post = createPost(1L, 1L);

        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        PostStatusResponse response = postService.changeStatus(1L, PostStatus.IN_PROGRESS, 1L);


        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("작성자가 아니면 FORBIDDEN_ACCESS 예외, 상태는 그대로")
    void changeStatus_noOwner_throws() {

        Post post = createPost(1L, 1L); // 작성자는 1번 회원

        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.changeStatus(1L, PostStatus.DONE,999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);

        assertThat(post.getStatus()).isEqualTo(PostStatus.OPEN);
    }

    @Test
    @DisplayName("회원 ID가 128 이상이어도 본인 확인이 동작(Long 캐시 경계)")
    void changeStatus_largeMemberId_success() {

        Post post = createPost(1L, 1000L);

        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        PostStatusResponse response = postService.changeStatus(1L, PostStatus.DONE, 1000L);

        assertThatThrownBy(() -> postService.changeStatus(999L, PostStatus.DONE, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("비로그인 조회는 조회수를 올리지 않음")
    void getDetail_anonymous_doesNotIncreaseViewCount() {

        Post post = createPost(1L, 1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        given(commentRepository.findByPostIdOrderByCreatedAtAsc(eq(1L), any(Pageable.class)))
                .willReturn(List.of());

        postService.getDetail(1L, null);

        verify(postViewService, never()).isFirstView(anyLong(), anyLong());
        verify(postRepository, never()).increaseViewCount(anyLong());
    }

    @Test
    @DisplayName("로그인 후 첫 조회면 조회수를 올림")
    void getDetail_firstView_increasesViewCount() {

        Post post = createPost(1L, 1L);
        given(postViewService.isFirstView(1L, 5L)).willReturn(true);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(eq(1L), any(Pageable.class)))
                .willReturn(List.of());

        postService.getDetail(1L, 5L);

        verify(postRepository).increaseViewCount(1L);
    }

    @Test
    @DisplayName("같은 회원이 다시 조회하면 조회수를 올리지 않음")
    void getDetail_duplicateView_doesNotIncreaseViewCount() {

        Post post = createPost(1L, 1L);
        given(postViewService.isFirstView(1L, 5L)).willReturn(false);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(eq(1L), any(Pageable.class)))
                .willReturn(List.of());

        postService.getDetail(1L, 5L);

        verify(postRepository, never()).increaseViewCount(anyLong());
    }

    @Test
    @DisplayName("댓글이 20건 미만이면 총 개수 조회를 생략")
    void getDetail_fewComments_skipsCountQuery() {

        Post post = createPost(1L, 1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        given(commentRepository.findByPostIdOrderByCreatedAtAsc(eq(1L), any(Pageable.class)))
                .willReturn(List.of(createComment(post),
                        createComment(post)));

        PostDetailResponse response = postService.getDetail(1L, null);

        assertThat(response.totalCommentCount()).isEqualTo(2);
        verify(commentRepository, never()).countByPostId(anyLong());
    }

    @Test
    @DisplayName("없는 게시글을 조회하면 POST_NOT_FOUND 예외")
    void getDetail_postNotFound_throws() {

        given(postRepository.findDetailById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getDetail(999L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 등록 시 작성자는 프록시만 가져옴")
    void create_usesReferenceNotFind() {

        Post post = createPost(1L, 1L);

        given(memberRepository.getReferenceById(1L)).willReturn(post.getMember());

        given(postRepository.save(any(Post.class))).willAnswer(i -> i.getArgument(0));

        PostCreateRequest request = new PostCreateRequest(
                PostType.LOST, "지갑 잃어버렸어요", "신흥역 4번 출구 근처입니다",
                PostCategory.WALLET, "신흥역 4번 출구", LocalDate.of(2026, 9, 14)
        );

        PostResponse response = postService.create(request, 1L, null);

        assertThat(response.title()).isEqualTo("지갑 잃어버렸어요");
        assertThat(response.status()).isEqualTo(PostStatus.OPEN);
        verify(memberRepository, never()).findById(anyLong());
    }



    private Post createPost(Long postId, Long memberId) {

        Member member = Member.builder()
                .email("test@test.com")
                .password("encoded")
                .nickname("테스터")
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);

        Post post = Post.builder()
                .member(member)
                .type(PostType.LOST)
                .title("지갑 잃어버렸어요")
                .content("강남역 2번 출구 근처입니다")
                .category(PostCategory.WALLET)
                .location("강남역 2번 출구")
                .lostFoundDate(LocalDate.of(2026, 9, 14))
                .build();
        ReflectionTestUtils.setField(post, "id", postId);

        return post;
    }


    private Comment createComment(Post post) {


        return Comment.builder()
                .post(post)
                .member(post.getMember())
                .content("혹시 검은색 반지갑인가요?")
                .build();
    }
}
