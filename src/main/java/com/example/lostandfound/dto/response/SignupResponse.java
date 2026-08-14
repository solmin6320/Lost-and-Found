package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Member;

import java.time.LocalDateTime;

// 회원가입 성공 시 클라이언트에게 줄 응답 DTO
// password 제외(Member 엔티티를 노출X)
public record SignupResponse(

        Long id,
        String email,
        String nickname,
        LocalDateTime createdAt
        )
{
    // Service 코드에서 필드를 하나씩 나열하지 않고 이 메서드 하나로 변환하도록 응집
    public static SignupResponse from(Member member) {
        return new SignupResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getCreatedAt()
        );
    }
}
