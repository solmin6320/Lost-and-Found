package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.PostCreateRequest;
import com.example.lostandfound.dto.request.PostSearchCondition;
import com.example.lostandfound.dto.response.PostDetailResponse;
import com.example.lostandfound.dto.response.PostListResponse;
import com.example.lostandfound.dto.response.PostResponse;
import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Post;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostViewService postViewService;

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

        return PostDetailResponse.from(post);
    }
}
