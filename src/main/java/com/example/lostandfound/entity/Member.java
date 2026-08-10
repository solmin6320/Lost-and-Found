package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 멤버 Entity
@Entity
@Table(name = "MEMBER") // 매핑할 실제 테이블명
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 사용에 필요한 최소한의 생성자
public class Member {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB에 PK 자동처리 위임
    @Column(name = "member_id") // 매핑할 실제 컬럼명
    private Long id;

    // 이메일
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 비밀번호
    @Column(nullable = false, length = 60)
    private String password;

    // 회원 닉네임
    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    // 생성 날짜
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 수정 날짜
    private LocalDateTime updatedAt;

    // Builder로만 객체 생성 가능하도록 강제(필드 임의 주입 방지)
    @Builder
    private Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.createdAt = LocalDateTime.now();
    }
}