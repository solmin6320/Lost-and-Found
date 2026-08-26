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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
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

    @Test
    @DisplayName("재발급 성공 시 새 액세스 토큰과 새 리프레시 토큰 쿠키를 반환")
    void reissue_success() {
        givenReissueSucceeds();

        AuthService.LoginResult result = authService.reissue(REFRESH_TOKEN);

        assertThat(result.response().accessToken()).isEqualTo("new-access-token");

        // 리프레시 토큰도 교체됨
        assertThat(result.cookie().getValue()).isNotEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("재발급 시 Redis에 새 리프레시 토큰을 덮어써 기존 토큰을 폐기")
    void reissue_rotatesRefreshToken() {
        givenReissueSucceeds();

        authService.reissue(REFRESH_TOKEN);

        // 저장된 값이 제출 토큰이 아닌 새 토큰이어야 로테이션이 성립
        ArgumentCaptor<String> savedToken = ArgumentCaptor.forClass(String.class);

        verify(refreshTokenService).save(eq(1L), savedToken.capture());

        assertThat(savedToken.getValue()).isEqualTo("new-refresh-token");

        assertThat(savedToken.getValue()).isNotEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없으면 검증을 시도하지 않고 401")
    void reissue_missingCookie() {

        // 스텁이 하나도 없음(null이면 뒤 조건을 평가하지 않음)
        assertThatThrownBy(() ->
                authService.reissue(null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verify(jwtTokenProvider, never()).validateToken(any());
    }

    @Test
    @DisplayName("만료되거나 위조된 리프레시 토큰이면 401")
    void reissue_invalidToken() {

        given(jwtTokenProvider.validateToken(REFRESH_TOKEN)).willReturn(false);

        assertThatThrownBy(() ->
                authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        // 조건 순서를 고정
        // isRefreshToken이 먼저 실행되면 만료 토큰이 500 예외를 던짐
        verify(jwtTokenProvider, never()).isRefreshToken(any());
    }

    @Test
    @DisplayName("액세스 토큰을 리프레시 토큰 자리에 제출하면 401")
    void reissue_accessTokenSubmitted() {

        given(jwtTokenProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(REFRESH_TOKEN)).willReturn(false); // category가 access

        // 액세스 토큰으로 무제한 재발급 차단
        assertThatThrownBy(() ->
                authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }



    @Test
    @DisplayName("Redis에 저장된 토큰과 일치하지 않으면 401")
    void reissue_redisMismatch() {

        given(jwtTokenProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getMemberId(REFRESH_TOKEN)).willReturn(1L);

        // 로그아웃, 타 기기 재로그인, 옛날 토큰 재사용이 모두 여기로 들어옴
        given(refreshTokenService.matches(1L, REFRESH_TOKEN)).willReturn(false);

        assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_MISMATCH);
    }

    @Test
    @DisplayName("재발급 실패 시 토큰을 발급하거나 Redis에 저장하지 않음")
    void reissue_failureDoesNotIssueToken() {

        given(jwtTokenProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getMemberId(REFRESH_TOKEN)).willReturn(1L);
        given(refreshTokenService.matches(1L, REFRESH_TOKEN)).willReturn(false);

        assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                .isInstanceOf(CustomException.class);

        verify(jwtTokenProvider, never()).createAccessToken(any());
        verify(refreshTokenService, never()).save(anyLong(), anyString());
    }

    @Test
    @DisplayName("로그아웃 시 Redis의 리프레시 토큰을 삭제")
    void logout_deletesRefreshToken() {
        authService.logout(1L);

        // 쿠키만 지워지고 재발급은 가능하게 하는것을 차단
        verify(refreshTokenService).delete(1L);
    }

    @Test
    @DisplayName("로그아웃 쿠키는 Max-Age=0이고 발급 때와 이름, 경로가 같음")
    void logout_expiresCookie() {
        ResponseCookie cookie = authService.logout(1L);

        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(0L); // 즉시 만료
        assertThat(cookie.getValue()).isEqualTo("");

        // 이름과 경로를 발급 때와 동일하게 작성
        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
    }


    // 재발급 테스트가 공유하는 토큰
    private static final String REFRESH_TOKEN = "valid-refresh-token";

    // 재발급 성공 경로에 필요한 스텁 모음
    private void givenReissueSucceeds() {

        given(jwtTokenProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getMemberId(REFRESH_TOKEN)).willReturn(1L);
        given(refreshTokenService.matches(1L, REFRESH_TOKEN)).willReturn(true);

        given(jwtTokenProvider.createAccessToken(any())).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("new-refresh-token");
        given(jwtProperties.accessTokenExpiration()).willReturn(300000L);
        given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);
    }
}
