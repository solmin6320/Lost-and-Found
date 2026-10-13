package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Claim;
import com.example.lostandfound.entity.ClaimStatus;
import com.example.lostandfound.entity.PostStatus;
import com.example.lostandfound.entity.PostType;

import java.time.LocalDateTime;

public record MyClaimResponse(

        Long id,
        Long postId,
        String postTitle,
        PostType postType,
        PostStatus postStatus,
        String question,
        String answer,
        ClaimStatus status,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        LocalDateTime receivedAt
) {

    public static MyClaimResponse from(Claim claim) {

        return new MyClaimResponse(
                claim.getId(),
                claim.getPost().getId(),
                claim.getPost().getTitle(),
                claim.getPost().getType(),
                claim.getPost().getStatus(),
                claim.getQuestion(),
                claim.getAnswer(),
                claim.getStatus(),
                claim.getCreatedAt(),
                claim.getDecidedAt(),
                claim.getReceivedAt()
        );
    }

}
