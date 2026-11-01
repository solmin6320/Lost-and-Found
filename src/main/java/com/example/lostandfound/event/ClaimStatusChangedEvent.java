package com.example.lostandfound.event;

import com.example.lostandfound.entity.ClaimStatus;

// 주인 확인 요청 상태가 바뀜
public record ClaimStatusChangedEvent(
        Long claimId,
        ClaimStatus status
) {
}
