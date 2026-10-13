package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Claim;
import com.example.lostandfound.entity.ClaimStatus;

import java.time.LocalDateTime;

public record ClaimResponse(

        Long id,
        Long postId,
        String question,
        String answer,
        ClaimStatus status,
        String claimantNickname,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        LocalDateTime receivedAt
) {

    public static ClaimResponse from(Claim claim) {

        return new ClaimResponse(
                claim.getId(),
                claim.getPost().getId(),
                claim.getQuestion(),
                claim.getAnswer(),
                claim.getStatus(),
                claim.getClaimant().getNickname(),
                claim.getCreatedAt(),
                claim.getDecidedAt(),
                claim.getReceivedAt()
        );
    }
}
