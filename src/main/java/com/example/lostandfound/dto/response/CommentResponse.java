package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(

        Long id,
        Long memberId,
        String nickname,
        String content,
        LocalDateTime createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getMember().getId(),
                comment.getMember().getNickname(), // @EntityGraph로 이미 로딩
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
