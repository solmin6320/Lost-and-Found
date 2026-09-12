package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostStatus;

import java.time.LocalDateTime;

// 상태 배지 갱신에 필요한 최소 정보
public record PostStatusResponse(

        Long id,
        PostStatus status,
        LocalDateTime updatedAt
) {

    public static PostStatusResponse from(Post post) {

        return new PostStatusResponse(
                post.getId(),
                post.getStatus(),
                post.getUpdatedAt()
        );
    }
}
