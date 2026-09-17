package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.NicknameUpdateRequest;
import com.example.lostandfound.dto.request.PasswordUpdateRequest;
import com.example.lostandfound.dto.request.SignupRequest;
import com.example.lostandfound.dto.response.MemberResponse;
import com.example.lostandfound.dto.response.SignupResponse;
import com.example.lostandfound.entity.Member;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원 관련 비즈니스 로직 담당
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt 해시 생성용
    private final RefreshTokenService refreshTokenService;

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

    // 조회 전용
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(Long memberId) {

        // 이메일, 닉네임을 읽어야 하므로 실제 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponse.from(member);
    }

    // 닉네임 변경
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public MemberResponse updateNickname(Long memberId, NicknameUpdateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 같은 닉네임이면 중복 검사에 자기 자신이 걸리므로 먼저 걸러냄
        if (member.getNickname().equals(request.nickname())) {
            return MemberResponse.from(member);
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        member.changeNickname(request.nickname());

        return MemberResponse.from(member);
    }

    // 비밀번호 변경
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void updatePassword(Long memberId, PasswordUpdateRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 현재 비밀번호를 다시 확인
        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        member.changePassword(passwordEncoder.encode(request.password()));

        // 탈취된 리프레시 토큰을 즉시 폐기
        refreshTokenService.delete(memberId);
    }
}
