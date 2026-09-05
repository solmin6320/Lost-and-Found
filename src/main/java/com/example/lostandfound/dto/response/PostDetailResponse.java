package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostCategory;
import com.example.lostandfound.entity.PostStatus;
import com.example.lostandfound.entity.PostType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        List<PostImageResponse> images
) {

    public static PostDetailResponse from(Post post) {

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
                        .map(PostImageResponse::from)
                        .toList()
        );
    }
}
