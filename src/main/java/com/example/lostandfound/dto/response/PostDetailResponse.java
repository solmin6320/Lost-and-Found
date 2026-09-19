package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

// 상세 전용
public record PostDetailResponse(

        Long id,
        Long memberId,
        String nickname,
        PostType type,
        String title,
        String content,
        PostCategory category,
        String location,
        LocalDate lostFoundDate,
        PostStatus status,
        int viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PostImageResponse> images,
         List<CommentResponse> comments,
        long totalCommentCount
) {

    public static PostDetailResponse from(Post post, List<Comment> comments, long totalCommentCount, String imageBaseUrl) {

        return new PostDetailResponse(
                post.getId(),
                post.getMember().getId(),
                post.getMember().getNickname(),
                post.getType(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getLocation(),
                post.getLostFoundDate(),
                post.getStatus(),
                post.getViewCount(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getImages().stream() // fetch join으로 이미 로딩됨
                        .map(image -> PostImageResponse.from(image, imageBaseUrl))
                        .toList(),
                comments.stream()
                        .map(CommentResponse::from)
                        .toList(),
                totalCommentCount
        );
    }
}
