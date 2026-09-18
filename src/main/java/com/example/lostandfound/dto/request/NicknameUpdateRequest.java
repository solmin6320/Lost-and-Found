package com.example.lostandfound.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 닉네임 변경 요청
public record NicknameUpdateRequest(

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(max = 20, message = "닉네임은 20자를 초과할 수 없습니다")
        String nickname
) {
}
