package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.CommentRequest;
import com.example.lostandfound.dto.response.CommentResponse;
import com.example.lostandfound.entity.*;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.CommentRepository;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// CommentService 단위 테스트
@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("없는 게시글에 댓글을 달면 404예외")
    void create_postNotFound() {

        given(postRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(1L, new CommentRequest("내용"), 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("댓글 등록 시 요청 내용과 작성자로 저장")
    void create_success() {

        Member member = createMember(1L);
        Post post = createPost(10L, member);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(memberRepository.getReferenceById(1L)).willReturn(member);
        given(commentRepository.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));

        CommentResponse response = commentService.create(10L, new CommentRequest("혹시 파란색?"), 1L);

        assertThat(response.content()).isEqualTo("혹시 파란색?");
        assertThat(response.memberId()).isEqualTo(1L);
    }


    @Test
    @DisplayName("없는 게시글의 댓글 목록을 조회하면 404예외")
    void getComments_postNotFound() {

        given(postRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> commentService.getComments(1L, 0))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 목록은 요청한 페이지를 20건 단위로 조회")
    void getComments_pageSizeFixed() {

        given(postRepository.existsById(1L)).willReturn(true);
        given(commentRepository.findPageByPostIdOrderByCreatedAtAscIdAsc(eq(1L), any(Pageable.class)))
                .willReturn(Page.empty());

        commentService.getComments(1L, 2);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(commentRepository).findPageByPostIdOrderByCreatedAtAscIdAsc(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("없는 댓글을 수정하면 404예외")
    void update_commentNotFound() {

        given(commentRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(100L, new CommentRequest("바꾼 내용"), 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("남의 댓글을 수정하면 403 예외, 내용은 그대로")
    void update_noOwner() {
        Comment comment = createComment(100L, createMember(1L));

        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(100L, new CommentRequest("바꾼 내용"), 999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);

        assertThat(comment.getContent()).isEqualTo("원래 내용");
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("작성자 본인이 수정하면 내용과 수정 시각이 바뀜")
    void update_owner() {

        Comment comment = createComment(100L, createMember(1L));

        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        CommentResponse response = commentService.update(100L, new CommentRequest("바꾼 내용"), 1L);

        assertThat(response.content()).isEqualTo("바꾼 내용");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("남의 댓글은 삭제하지 않고 403 예외")
    void delete_notOwner() {

        Comment comment = createComment(100L, createMember(1L));

        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(100L, 999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);

        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("작성자 본인이면 댓글 삭제")
    void delete_owner() {

        Comment comment = createComment(100L, createMember(1L));

        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        commentService.delete(100L, 1L);

        verify(commentRepository).delete(comment);
    }



    private Member createMember(Long memberId) {

        Member member = Member.builder()
                .email("test@test.com")
                .password("encoded")
                .nickname("테스트")
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);

        return member;
    }

    private Post createPost(Long postId, Member member) {

        Post post = Post.builder()
                .member(member)
                .type(PostType.LOST)
                .title("지갑 잃어버렸어요")
                .content("신흥역 4번 출구 근처입니다")
                .category(PostCategory.WALLET)
                .location("신흥역 4번 출구")
                .lostFoundDate(LocalDate.of(2026, 9, 26))
                .build();
        ReflectionTestUtils.setField(post, "id", postId);

        return post;
    }

    private Comment createComment(Long commentId, Member member) {

        Comment comment = Comment.builder()
                .post(createPost(10L, member))
                .member(member)
                .content("원래 내용")
                .build();
        ReflectionTestUtils.setField(comment, "id", commentId);

        return comment;
    }

}
