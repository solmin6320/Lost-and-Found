package com.example.lostandfound.security;

import com.example.lostandfound.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Member member;

    // 원본 엔티티 접근용
    public Member getMember() {
        return member;
    }

    // 편의 메서드
    // Id를 Long 타입 그대로 꺼냄
    public Long getMemberId() {
        return member.getId();
    }

    // 사용자 권한 목록
    // DB 명세서에 role 컬럼이 없기 때문에 모든 회원이 동일하게 ROLE_USER
    // 추후 관리자 페이지 등 확장 가능
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );
    }

    // 비밀번호 대조 시 사용
    // DB에 저장된 BCrypt 해시값을 반환
    @Override
    public String getPassword() {
        return member.getPassword();
    }

    // 사용자 주체
    // 이메일이 아닌 Id(PK)를 반환
    @Override
    public String getUsername() {
        return String.valueOf(member.getId());
    }

}
