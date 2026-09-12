package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.PostCreateRequest;
import com.example.lostandfound.dto.request.PostSearchCondition;
import com.example.lostandfound.dto.response.PostDetailResponse;
import com.example.lostandfound.dto.response.PostListResponse;
import com.example.lostandfound.dto.response.PostResponse;
import com.example.lostandfound.dto.response.PostStatusResponse;
import com.example.lostandfound.entity.Comment;
import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostStatus;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.CommentRepository;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostViewService postViewService;
    private final CommentRepository commentRepository;

    // 상세 화면에 먼저 보여줄 댓글 수
    private static final int COMMENT_PREVIEW_SIZE = 20;

    // 경로 규칙이 바뀌어도 스스로를 지키도록 선언
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PostResponse create(PostCreateRequest request, Long memberId) {

        // FK 값만 필요하므로 프록시만 가져옴
        Member member = memberRepository.getReferenceById(memberId);


        Post post = Post.builder()
                .member(member)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .location(request.location())
                .lostFoundDate(request.lostFoundDate())
                .build();

        Post postSaved = postRepository.save(post);

        return PostResponse.from(postSaved);
    }

    // 비로그인도 열람 가능
    @Transactional(readOnly = true)
    public Page<PostListResponse> search(PostSearchCondition condition, Pageable pageable) {

        return postRepository.search(condition, pageable)
                .map(PostListResponse::from);
    }

    // 조회수 증가가 섞이므로 readOnly를 쓰지 않음
    @Transactional
    public PostDetailResponse getDetail(Long postId, Long memberId) {

        // 비로그인은 중복 판정이 불가해 집계하지 않음
        if (memberId != null && postViewService.isFirstView(postId, memberId)) {
            postRepository.increaseViewCount(postId);
        }

        // 증가 뒤에 조회해야 응답에 증가된 값이 담김
        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 이미지와 따로 조회
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(
                postId, PageRequest.of(0, COMMENT_PREVIEW_SIZE)
        );

        // 개수가 자명하므로 COUNT를 생략
        long totalCommentCount = (comments.size() < COMMENT_PREVIEW_SIZE) ? comments.size() : commentRepository.countByPostId(postId);


        return PostDetailResponse.from(post, comments, totalCommentCount);
    }

    // 소유자 검증이 필요하므로 처리
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PostStatusResponse changeStatus(Long postId, PostStatus status, Long memberId) {

        // 게시글 상태와 작성자를 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 작성자 본인만 변경 가능
        if (!post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 전이 규칙 판단은 엔티티가 담당
        post.changeStatus(status);

        return PostStatusResponse.from(post);
    }
}
