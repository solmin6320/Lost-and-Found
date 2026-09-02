package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.PostCreateRequest;
import com.example.lostandfound.dto.response.PostResponse;
import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Post;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

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
}
