package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.CommentRequest;
import com.example.lostandfound.dto.response.CommentResponse;
import com.example.lostandfound.entity.Comment;
import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Post;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.CommentRepository;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    private static final int PAGE_SIZE = 20;

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public CommentResponse create(Long postId, CommentRequest request, Long memberId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 토큰으로 검증된 회원이라 프록시로 충분
        Member member = memberRepository.getReferenceById(memberId);

        Comment comment = Comment.builder()
                .post(post)
                .member(member)
                .content(request.content())
                .build();

        return CommentResponse.from(commentRepository.save(comment));
    }

    // 비로그인도 열람 가능
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long postId, int page) {

        if (!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        // size와 정렬은 서버가 고정
        return commentRepository.findPageByPostIdOrderByCreatedAtAscIdAsc(postId, PageRequest.of(page, PAGE_SIZE))
                .map(CommentResponse::from);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public CommentResponse update(Long commentId, CommentRequest request, Long memberId) {

        Comment comment = findOwnedComment(commentId, memberId);

        comment.update(request.content());

        return CommentResponse.from(comment);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void delete(Long commentId, Long memberId) {

        commentRepository.delete(findOwnedComment(commentId, memberId));
    }


    // 존재 확인 후 작성자 본인인지 검증
    private Comment findOwnedComment(Long commentId, Long memberId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return comment;
    }
}
