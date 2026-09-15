package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Member;

import java.time.LocalDateTime;

// 내 정보 조회 응답
public record MemberResponse(

        Long id,
        String email,
        String nickname,
        LocalDateTime createdAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getCreatedAt()
        );
    }
}
