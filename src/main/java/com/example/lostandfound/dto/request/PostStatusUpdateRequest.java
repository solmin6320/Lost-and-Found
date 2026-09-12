package com.example.lostandfound.dto.request;

import com.example.lostandfound.entity.PostStatus;
import jakarta.validation.constraints.NotNull;

// 변경할 상태만 받음
public record PostStatusUpdateRequest(

        @NotNull(message = "상태는 필수입니다")
        PostStatus status
) {
}
