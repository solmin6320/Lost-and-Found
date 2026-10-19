package com.example.lostandfound.security;

import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final Member member;
    private final String password;
    private final Role role;

    // 원본 엔티티 접근용
    public Member getMember() {
        return member;
    }

    // 로그인 용(DB 조회한 엔티티로 생성)
    public CustomUserDetails(Member member) {
        this.id = member.getId();
        this.password = member.getPassword();
        this.member = member;
        this.role = member.getRole();
    }

    // 필터, 재발급용(토큰의 id와 역할로 생성)
    public CustomUserDetails(Long id, Role role) {
        this.id = id;
        this.password = null;
        this.member = null;
        this.role = role;
    }


    // 편의 메서드
    // Id를 Long 타입 그대로 꺼냄
    public Long getMemberId() {
        return id;
    }

    // 회원 역할
    public Role getRole() {
        return role;
    }

    // 사용자 권한 목록
    // 권한 기능 추가
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    // 비밀번호 대조 시 사용
    // DB에 저장된 BCrypt 해시값을 반환
    @Override
    public String getPassword() {
        return password;
    }

    // 사용자 주체
    // 이메일이 아닌 Id(PK)를 반환
    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

}
