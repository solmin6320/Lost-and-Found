package com.example.lostandfound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// record 로 생성자, 접근자, toString 등 자동 생성
public record SignupRequest(

        // 이메일 형식 검사
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다") // Member 엔티티 length = 100과 동일하게 일치
    String email,

    // 비밀번호 형식 검사
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다") // 평문 비밀번호 자체에 대한 형식 검사(아직 BCrypt 전)
    String password,

    // 회원 닉네임 형식 검사
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 20, message = "닉네임은 20자를 초과할 수 없습니다")
    String nickname

    ) {}