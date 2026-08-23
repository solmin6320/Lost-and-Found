package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.LoginRequest;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.jwt.JwtProperties;
import com.example.lostandfound.jwt.JwtTokenProvider;
import com.example.lostandfound.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 로그인 분기 로직 단위 테스트
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private AuthService authService;

    // 모든 테스트가 동일한 요청을 사용하므로 필드로 추출
    // MemberServiceTest 보다 간편해짐(중복 제거)
    private final LoginRequest request = new LoginRequest("test@test.com", "password123");


    @Test
    @DisplayName("로그인 성공 시 액세스 토큰과 리프레시 토큰 쿠키를 반환")
    void login_success() {

        // 잠금 상태가 아니고 인증이 성공하는 상황을 설정
        given(loginAttemptService.locked(request.email())).willReturn(false); // 잠금 아님
        given(authenticationManager.authenticate(any())).willReturn(authentication); // 인증 성공
        given(authentication.getPrincipal()).willReturn(userDetails); // 주체 반환
        given(userDetails.getMemberId()).willReturn(1L); // 회원 ID
        given(jwtTokenProvider.createAccessToken(authentication)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(authentication)).willReturn("refresh-token");
        given(jwtProperties.accessTokenExpiration()).willReturn(300000L);
        given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);

        // 살재 로그인 로직
        AuthService.LoginResult result = authService.login(request);

        // 응답 바디에 액세스 토큰 관련 정보가 담겼는지 확인
        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.response().tokenType()).isEqualTo("Bearer");
        assertThat(result.response().accessTokenExpires()).isEqualTo(300000L);

        // 리프레시 토큰은 응답 바디가 아닌 쿠키로만 전달
        assertThat(result.cookie().getValue()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("로그인 성공 시 리프레시 토큰을 Redis에 저장하고 실패 카운터를 초기화")
    void login_savesRefreshTokenAndResetsCounter() {

        given(loginAttemptService.locked(request.email())).willReturn(false);
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(userDetails.getMemberId()).willReturn(1L);
        given(jwtTokenProvider.createAccessToken(authentication)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(authentication)).willReturn("refresh-token");
        given(jwtProperties.accessTokenExpiration()).willReturn(300000L);
        given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);

        authService.login(request);

        // 반환값이 아닌 협력 객체를 호출 했는지를 검증
        verify(refreshTokenService).save(1L, "refresh-token"); // 레디스에 리프레시 토큰 저장

        verify(loginAttemptService).reset(request.email()); // 실패 카운트 초기화
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키에 httpOnly, secure, SameSite가 설정")
    void login_cookieSecurityAttributes() {

        given(loginAttemptService.locked(request.email())).willReturn(false);
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(userDetails.getMemberId()).willReturn(1L);
        given(jwtTokenProvider.createAccessToken(authentication)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(authentication)).willReturn("refresh-token");
        given(jwtProperties.accessTokenExpiration()).willReturn(300000L);
        given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);

        AuthService.LoginResult result = authService.login(request);

        // 보안 설정을 테스트로 고정
        assertThat(result.cookie().isHttpOnly()).isTrue(); // JS 접근 차단(XSS 방어)
        assertThat(result.cookie().isSecure()).isTrue(); // HTTPS 전용
        assertThat(result.cookie().getSameSite()).isEqualTo("Strict"); // 외부 요청 미첨부(CSRF 방어)
        assertThat(result.cookie().getPath()).isEqualTo("/api/auth"); // 노출 경로 최소화
    }

    @Test
    @DisplayName("계정이 잠긴 상태면 인증을 시도하지 않고 예외를 던진다")
    void login_accountLocked() {

        // 이미 5회 이상 실패해 잠긴 상태
        given(loginAttemptService.locked(request.email())).willReturn(true);

        // 423 Locked에 해당하는 예외가 발생
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        // 잠긴 계정은 BCrypt 연산과 DB 조회를 아예 하지 않아야 함
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("인증에 실패하면 실패 카운트를 증가시키고 예외를 던짐")
    void login_invalidCredentials() {

        // 잠금은 아니지만 비밀번호가 틀린 상황
        given(loginAttemptService.locked(request.email())).willReturn(false);
        willThrow(new BadCredentialsException("자격 증명 실패"))
                .given(authenticationManager).authenticate(any());

        // CustomException으로 변환
        // 회원 없음과 비밀번호 불일치를 구분하지 않음(열거 공격을 방어)
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        // 실패 카운트가 올라가지 않으면 브루트포스 공격을 방어하지 못함
        verify(loginAttemptService).recordFailure(request.email());
    }

    @Test
    @DisplayName("인증에 실패하면 토큰을 발급하거나 Redis에 저장하지 않음")
    void login_failureDoesNotIssueToken() {
        given(loginAttemptService.locked(request.email())).willReturn(false);
        willThrow(new BadCredentialsException("자격 증명 실패"))
                .given(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class);

        // 인증 실패인데 토큰이 발급되면 안되므로 명시적으로 검증
        verify(jwtTokenProvider, never()).createAccessToken(any());
        verify(jwtTokenProvider, never()).createRefreshToken(any());
        verify(refreshTokenService, never()).save(anyLong(), anyString());
    }

}
