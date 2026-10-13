package com.example.lostandfound.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 주인 확인 요청
public record ClaimRequest(

        @NotBlank(message = "답은 필수입니다")
        @Size(max = 500, message = "답은 500자를 초과할 수 없습니다")
        String answer
) {

    // 앞뒤 공백 제거
    public ClaimRequest {
        if (answer != null) {
            answer = answer.strip();
        }
    }
}
