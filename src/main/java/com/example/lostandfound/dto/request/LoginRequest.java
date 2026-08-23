package com.example.lostandfound.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 회원가입과 달리 길이, 형식 제약을 두지 않음
// 비밀번호 정책을 응답으로 노출하지 않음
public record LoginRequest(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        String password
) {
}
