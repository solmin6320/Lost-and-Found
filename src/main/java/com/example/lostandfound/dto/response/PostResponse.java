package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostCategory;
import com.example.lostandfound.entity.PostStatus;
import com.example.lostandfound.entity.PostType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PostResponse(

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
        LocalDateTime updatedAt
)
{
    public static PostResponse from(Post post) {

        return new PostResponse(
                post.getId(),
                post.getMember().getId(), // 프록시의 ID는 조회 없이 꺼냄
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
                post.getUpdatedAt()
        );
    }
}
