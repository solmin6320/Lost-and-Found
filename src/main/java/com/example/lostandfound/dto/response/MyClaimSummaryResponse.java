package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Claim;
import com.example.lostandfound.entity.ClaimStatus;

// 상세에서 보는 내 요청
public record MyClaimSummaryResponse(

        Long id,
        ClaimStatus status
) {

    public static MyClaimSummaryResponse from(Claim claim) {
        return new MyClaimSummaryResponse(claim.getId(), claim.getStatus());
    }
}
