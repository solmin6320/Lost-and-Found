package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Claim;

import java.time.LocalDateTime;

// 되찾기 공개 기록
public record ReturnRecordResponse(
    LocalDateTime requestedAt,
    LocalDateTime approvedAt,
    LocalDateTime receivedAt
) {

    // 받음 처리된 요청에서만 만듦
     public static ReturnRecordResponse from(Claim claim) {
         return new ReturnRecordResponse(
                 claim.getCreatedAt(),
                 claim.getDecidedAt(),
                 claim.getReceivedAt()
         );
     }
}
