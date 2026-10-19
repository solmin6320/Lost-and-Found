package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Role;

import java.time.LocalDateTime;

// 관리자 회원 검색 응답
public record AdminMemberResponse(

        Long id,
        String email,
        String nickname,
        Role role,
        LocalDateTime createdAt
) {

    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}
