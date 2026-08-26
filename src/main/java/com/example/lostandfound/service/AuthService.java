package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.LoginRequest;
import com.example.lostandfound.dto.response.LoginResponse;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.jwt.JwtProperties;
import com.example.lostandfound.jwt.JwtTokenProvider;
import com.example.lostandfound.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    // 로그인 결과를 담는 내부 전달용 객체
    public record LoginResult(LoginResponse response, ResponseCookie cookie) {

    }

    public LoginResult login(LoginRequest request) {

        // 잠긴 계정은 인증 시도를 하지 않음
        if (loginAttemptService.locked(request.email())) {
            throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
        }

        Authentication authentication = authenticate(request);

        // 인증 성공(실패 카운트 초기화)
        loginAttemptService.reset(request.email());

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        // 리프레시 토큰을 SHA-256 해시로 Redis에 저장
        Long id = ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
        refreshTokenService.save(id, refreshToken);

        return new LoginResult(
                LoginResponse.of(accessToken, jwtProperties.accessTokenExpiration()),
                buildRefreshTokenCookie(refreshToken)
        );
    }

    // 이메일, 비밀번호 인증 수행(실패시 카운트를 올리고 401 반환)
    private Authentication authenticate(LoginRequest request) {
        try {
            // 아직 인증되지 않은 요청 객체
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(request.email());

            // 회원 없음과 비밀번호 불일치를 구분하지 않음
            // 열거 공격 방어
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // 리프레시 토큰으로 액세스, 리프레시 재발급(로테이션)
    public LoginResult reissue(String refreshToken) {

        // validateToken을 먼저 호출해야 함(만료 토큰이 먼저 닿으면 401이 아닌 500이 됨)
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) { // 액세스 제출 차단

            // 쿠키 없음, 만료, 위조, 종류 불일치를 구분하지 않음
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        // 서명이 검증된 토큰이므로 sub를 신뢰할 수 있음
        Long id = jwtTokenProvider.getMemberId(refreshToken);
        
        // Redis 해시와 대조(옛날 토큰 재사용을 차단)
        if (!refreshTokenService.matches(id, refreshToken)) {

            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }
        Authentication authentication = buildAuthentication(id);

        String newAccessToken =
                jwtTokenProvider.createAccessToken(authentication);
        String newRefreshToken =
                jwtTokenProvider.createRefreshToken(authentication);

        // 같은 키를 덮어써 폐기와 발급을 한 번에 처리
        refreshTokenService.save(id, newRefreshToken);

        return new LoginResult(
                LoginResponse.of(newAccessToken, jwtProperties.accessTokenExpiration()),
                buildRefreshTokenCookie(newRefreshToken)
        );
    }

    // 로그아웃(Redis의 리프레시 토큰 삭제 + 쿠키 만료)
    public ResponseCookie logout(Long id) {

        // 키가 없어도 예외가 아님(중복 로그아웃 허용)
        refreshTokenService.delete(id);

        return buildExpiredRefreshTokenCookie();
    }

    // 브라우저가 기존 쿠키를 버리도록 Max-Age=0으로 덮어씀
    private ResponseCookie buildExpiredRefreshTokenCookie() {

        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "") // 값은 비움
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth") // 발급 시와 동일
                .maxAge(0) // 즉시 만료
                .build();
    }



    // 토큰 발급에 필요한 최소 인증 객체 구성
    private Authentication buildAuthentication(Long id) {
        CustomUserDetails userDetails = new CustomUserDetails(id);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null, // 토큰으로 이미 검증됨
                userDetails.getAuthorities()
        );
    }



    // 리프레시 토큰을 담을 httpOnly 쿠키 생성
    private ResponseCookie buildRefreshTokenCookie(String refreshToken) {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
            .httpOnly(true) // JS 접근 차단(XSS 방어)
            .secure(true) // HTTPS에서만 전송(테스트 시 false 필요)
            .sameSite("Strict") // 외부 사이트 요청에 미첨부(CSRF 방어)
            .path("/api/auth") // 인증 관련 경로에만 전송
            .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpiration()))
            .build();
    }
}
