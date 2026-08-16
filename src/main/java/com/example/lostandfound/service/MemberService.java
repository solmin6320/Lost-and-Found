package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.SignupRequest;
import com.example.lostandfound.dto.response.SignupResponse;
import com.example.lostandfound.entity.Member;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원 관련 비즈니스 로직 담당
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt 해시 생성용(Bean 등록은 SecurityConfig에서 추가할 예정)

    // 트랜잭션으로 중간에 예외 발생 시 지금까지의 DB 변경을 롤백
    @Transactional
    public SignupResponse signup(SignupRequest request) {


        // 이메일 중복 검사
        if (memberRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 닉네임 중복 검사
        if (memberRepository.existsByNickname(request.nickname())) {
            throw  new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // DB 저장용
        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // 평문 비밀번호 -> BCrypt 해시
                .nickname(request.nickname())
                .build();

        // DB에 실제 저장
        Member memberSaved = memberRepository.save(member);

        // 저장된 엔티티를 응답 DTO로 변환(password는 제외)
        return SignupResponse.from(memberSaved);
    }
}
