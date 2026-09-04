package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostCategory;
import com.example.lostandfound.entity.PostStatus;
import com.example.lostandfound.entity.PostType;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 목록 전용(content 제외)
public record PostListResponse(

        Long id,
        String nickname,
        PostType type,
        String title,
        PostCategory category,
        String location,
        LocalDate lostFoundDate,
        PostStatus status,
        int viewCount,
        LocalDateTime createdAt
) {

    public static PostListResponse from(Post post) {

        return new PostListResponse(
                post.getId(),
                post.getMember().getNickname(), // fetch join으로 이미 로딩됨

                post.getType(),
                post.getTitle(),
                post.getCategory(),
                post.getLocation(),
                post.getLostFoundDate(),
                post.getStatus(),
                post.getViewCount(),
                post.getCreatedAt()
        );
    }
}
