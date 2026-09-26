package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.PostCreateRequest;
import com.example.lostandfound.dto.request.PostUpdateRequest;
import com.example.lostandfound.dto.response.PostDetailResponse;
import com.example.lostandfound.dto.response.PostResponse;
import com.example.lostandfound.dto.response.PostStatusResponse;
import com.example.lostandfound.entity.*;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.CommentRepository;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.PostImageRepository;
import com.example.lostandfound.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.*;


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
    private PostImageRepository postImageRepository;

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
        postService = new PostService(postRepository, memberRepository, postViewService, commentRepository, postImageRepository,s3Service, awsProperties);
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

        given(commentRepository.findByPostIdOrderByCreatedAtAscIdAsc(
eq(1L), any(Pageable.class)))
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
        given(commentRepository.findByPostIdOrderByCreatedAtAscIdAsc(
eq(1L), any(Pageable.class)))
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
        given(commentRepository.findByPostIdOrderByCreatedAtAscIdAsc(
eq(1L), any(Pageable.class)))
                .willReturn(List.of());

        postService.getDetail(1L, 5L);

        verify(postRepository, never()).increaseViewCount(anyLong());
    }

    @Test
    @DisplayName("댓글이 20건 미만이면 총 개수 조회를 생략")
    void getDetail_fewComments_skipsCountQuery() {

        Post post = createPost(1L, 1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        given(commentRepository.findByPostIdOrderByCreatedAtAscIdAsc(
eq(1L), any(Pageable.class)))
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

    @Test
    @DisplayName("상세 응답의 이미지 주소는 baseUrl + 객체 키")
    void getDetail_imageUrl_isBaseUrlPlusKey() {

        Post post = createPost(1L, 1L);
        post.addImage(PostImage.builder()
                        .originalFilename("지갑.jpg")
                        .storedFilename("aaa.jpg")
                        .filePath("posts/1/aaa.jpg") // DB에는 키만 저장
                        .fileSize(1024)
                        .build());

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAscIdAsc(
eq(1L), any(Pageable.class)))
                .willReturn(List.of());

        PostDetailResponse response = postService.getDetail(1L, null);

        assertThat(response.images().getFirst().url())
                .isEqualTo("https://cdn.test/posts/1/aaa.jpg");
    }

    @Test
    @DisplayName("이미지를 올라면 게시글 ID로 S3에 업로드")
    void create_withImages_uploadsWithPostId() {

        Post post = createPost(1L, 1L);

        given(memberRepository.getReferenceById(1L)).willReturn(post.getMember());

        given(postRepository.save(any(Post.class))).willAnswer(i -> {
            Post saved = i.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L); // DB가 채워주는 ID를 흉내

            return saved;
        });

        given(s3Service.upload(any(MultipartFile.class), eq(1L)))
                .willReturn("posts/1/aaa.jpg", "posts/1/bbb.png");

        List<MultipartFile> images = List.of(createFile("지갑.jpg"), createFile("뒷면.png"));

        postService.create(createRequest(), 1L, images);

        verify(s3Service, times(2)).upload(any(MultipartFile.class), eq(1L));
    }

    private PostCreateRequest createRequest() {

        return new PostCreateRequest(
                PostType.LOST, "지갑 잃어버렸어요", "신흥역 4번 출구 근처입니다",
                PostCategory.WALLET, "신흥역 4번 출구",
                LocalDate.of(2026, 9, 14)
        );
    }

    @Test
    @DisplayName("이미지가 5장을 넘으면 업로드 없이 예외 처리")
    void create_exceedsMaxImageCount_throws() {

        List<MultipartFile> images = List.of(
                createFile("1.jpg"), createFile("2.jpg"),
                createFile("3.jpg"), createFile("4.jpg"),
                createFile("5.jpg"), createFile("6.jpg")
        );

        assertThatThrownBy(() -> postService.create(createRequest(), 1L, images))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXCEEDED_IMAGE_COUNT);

        // 검사가 업로드보다 앞서야 S3에 쓰레기가 남지 않음
        verify(s3Service, never()).upload(any(MultipartFile.class), anyLong());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("빈 파트는 업로드 대상에서 제외")
    void create_emptyPart_isSkipped() {

        Post post = createPost(1L, 1L);

        given(memberRepository.getReferenceById(1L)).willReturn(post.getMember());

        given(postRepository.save(any(Post.class))).willAnswer(i -> {
            Post saved = i.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        given(s3Service.upload(any(MultipartFile.class), eq(1L)))
                .willReturn("posts/1/aaa.jpg");

        List<MultipartFile> images = List.of(
                new MockMultipartFile("images", "", "image/jpeg", new byte[0]), createFile("지갑.jpg"));

        postService.create(createRequest(), 1L, images);

        verify(s3Service, times(1)).upload(any(MultipartFile.class), eq(1L));

    }

    @Test
    @DisplayName("이미지를 안 보내면 기존 이미지를 건드리지 않음")
    void update_noImages_keepExistingImages() {

        Post post = createPostWithImage(1L, 1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        postService.update(1L, createUpdateRequest(false), null, 1L);

        assertThat(post.getImages()).hasSize(1);
        verify(s3Service, never()).deleteAfterCommit(any());
        verify(s3Service, never()).upload(any(MultipartFile.class), anyLong());
    }

    @Test
    @DisplayName("빈 파트만 와도 기존 이미지를 건드리지 않음")
    void update_emptyPartOnly_keepsExistingImages() {

        Post post = createPostWithImage(1L, 1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        List<MultipartFile> images = List.of(
                new MockMultipartFile("images", "", "images/jpeg", new byte[0]));

        postService.update(1L, createUpdateRequest(false), images, 1L);
        
        // 빈 파트를 삭제 신호로 읽으면 여기서 이미지가 사라짐
        assertThat(post.getImages()).hasSize(1);
        verify(s3Service, never()).deleteAfterCommit(any());
    }

    @Test
    @DisplayName("이미지를 보내면 교체하고 옛 객체 키는 커밋 후 삭제로 넘김")
    void update_withImages_replaceAndDeletesOldKeys() {

        Post post = createPostWithImage(1L, 1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));
        given(s3Service.upload(any(MultipartFile.class), eq(1L)))
                .willReturn("posts/1/new.jpg");

        postService.update(1L, createUpdateRequest(false),
                List.of(createFile("새 사진.jpg")), 1L);

        assertThat(post.getImages()).hasSize(1);

        assertThat(post.getImages().getFirst().getFilePath()).isEqualTo("posts/1/new.jpg");

        verify(s3Service).deleteAfterCommit(List.of("posts/1/old.jpg"));
    }

    @Test
    @DisplayName("removeImages가 true면 업로드 없이 전부 삭제")
    void update_removeImages_deletesAll() {

        Post post = createPostWithImage(1L, 1L);
        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        postService.update(1L, createUpdateRequest(true), null, 1L);

        assertThat(post.getImages()).isEmpty();
        verify(s3Service).deleteAfterCommit(List.of("posts/1/old.jpg"));
        verify(s3Service, never()).upload(any(MultipartFile.class), anyLong());
    }

    @Test
    @DisplayName("수정 시 5장을 넘으면 기존 이미지를 건드리지 않고 예외")
    void update_exceedsMaxImageCount_throws() {

        Post post = createPostWithImage(1L,1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        List<MultipartFile> images = List.of(
                createFile("1.jpg"), createFile("2.jpg"), createFile("3.jpg"),
                createFile("4.jpg"), createFile("5.jpg"), createFile("6.jpg")
        );

        assertThatThrownBy(() -> postService.update(1L, createUpdateRequest(false), images, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXCEEDED_IMAGE_COUNT);

        // 개수 검사가 우선이여야 함
        assertThat(post.getImages()).hasSize(1);
        verify(s3Service, never()).upload(any(MultipartFile.class), anyLong());
        verify(s3Service, never()).deleteAfterCommit(any());
    }

    @Test
    @DisplayName("작성자가 아니면 수정 시 403 예외")
    void update_notOwner_throws() {

        Post post = createPostWithImage(1L, 1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(1L, createUpdateRequest(true), null, 999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);

        // 권한 검사가 먼저
        assertThat(post.getImages()).hasSize(1);
        verify(s3Service, never()).deleteAfterCommit(any());
    }

    @Test
    @DisplayName("없는 게시글을 수정하면 404 예외")
    void update_postNotFound_throws() {

        given(postRepository.findDetailById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.update(999L, createUpdateRequest(false), null, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }


    @Test
    @DisplayName("삭제 시 댓글을 게시글보다 먼저 지움")
    void delete_removesCommentsBeforePost() {

        Post post = createPostWithImage(1L, 1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        postService.delete(1L, 1L);

        // 순서가 뒤바뀌면 FK 제약 위반으로 500 예외
        InOrder inOrder = inOrder(commentRepository, postRepository);
        inOrder.verify(commentRepository).deleteByPostId(1L);
        inOrder.verify(postRepository).delete(post);

        verify(s3Service).deleteAfterCommit(List.of("posts/1/old.jpg"));
    }

    @Test
    @DisplayName("작성자가 아니면 삭제 시 403 예외")
    void delete_notOwner_throws() {

        Post post =createPostWithImage(1L, 1L);

        given(postRepository.findDetailById(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.delete(1L, 999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);

        verify(commentRepository, never()).deleteByPostId(anyLong());
        verify(postRepository, never()).delete(any(Post.class));
        verify(s3Service, never()).deleteAfterCommit(any());
    }

    @Test
    @DisplayName("없는 게시글을 삭제하면 404 예외")
    void delete_postNotFound_throws() {

        given(postRepository.findDetailById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(999L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }


    private MultipartFile createFile(String filename) {

        return new MockMultipartFile("images", filename, "images/jpeg", "dummy".getBytes());
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

   private Post createPostWithImage(Long postId, Long memberId) {

        Post post= createPost(postId, memberId);
        post.addImage(PostImage.builder()
                        .originalFilename("지갑.jpg")
                        .storedFilename("old.jpg")
                        .filePath("posts/1/old.jpg")
                        .fileSize(1024)
                .build());

        return post;
   }

   private PostUpdateRequest createUpdateRequest(boolean removeImages) {

        return new PostUpdateRequest(
                PostType.LOST, "제목을 고쳤습니다", "본문도 고쳤습니다",
                PostCategory.WALLET, "신흥역 4번 출구",
                LocalDate.of(2026, 9, 14), removeImages
        );
   }
}
