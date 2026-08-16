package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.SignupRequest;
import com.example.lostandfound.dto.response.SignupResponse;
import com.example.lostandfound.entity.Member;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// MemberService의 회원가입 로직 단위 테스트
// 실제 DB를 쓰지 않아 매우 빠르게 실행
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository; // 가짜 Repository

    @Mock
    private PasswordEncoder passwordEncoder; // 가짜 인코더

    @InjectMocks
    private MemberService memberService; // 위 두 Mock이 주입된 서비스


    @Test
    @DisplayName("회원가입 성공 시 저장된 회원 정보를 담은 SignupResponse를 반환")
    void signup_success() {
        // 중복이 없고, 비밀번호 인코딩이 정상 동작하는 상황
        SignupRequest request = new SignupRequest("test@test.com", "1234@@", "김김김");

        given(memberRepository.existsByEmail(request.email())).willReturn(false);
        given(memberRepository.existsByNickname(request.nickname())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encoded");

        Member member = Member.builder()
                .email(request.email())
                .password("encoded")
                .nickname(request.nickname())
                .build();
        given(memberRepository.save(any(Member.class))).willReturn(member);


        SignupResponse response = memberService.signup(request);

        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.nickname()).isEqualTo(request.nickname());
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("회원가입 시 비밀번호는 평문이 아닌 인코딩 값으로 저장")
    void signup_passwordEncoded() {

        SignupRequest request = new SignupRequest("test@test.com", "password", "김김김");

        given(memberRepository.existsByEmail(request.email())).willReturn(false);
        given(memberRepository.existsByNickname(request.nickname())).willReturn(false);
        given(passwordEncoder.encode("password")).willReturn("encoded");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        memberService.signup(request);

        // encode()가 평문으로 호출되었는지 검증
        verify(passwordEncoder).encode("password");
    }

    @Test
    @DisplayName("이메일이 중복되면 DUPLICATE_EMAIL 예외를 던지고 저장을 하지 않는다")
    void signup_Email() {
        // 이메일이 이미 존재하는 상황
        SignupRequest request = new SignupRequest("test@test.com", "password", "김김김");

        given(memberRepository.existsByEmail(request.email())).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        // 예외가 던져졌으므로 저장이 되면 안됌
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("닉네임이 중복되면 DUPLICATE_NICKNAME 예외를 던지고 저장을 하지 않는다")
    void signup_Nickname() {
        // 이메일을 통과하지만 닉네임이 중복인 상황
        SignupRequest request = new SignupRequest("test@test.com", "1234", "김김김");

        given(memberRepository.existsByEmail(request.email())).willReturn(false);
        given(memberRepository.existsByNickname(request.nickname())).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

        // 예외가 던져졌으므로 저장이 되면 안됌
        verify(memberRepository, never()).save(any(Member.class));
    }

}
