package com.example.lostandfound.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    // 생성자에서 SecretKey를 한 번만 만들어 필드에 보관
    // (매 토큰 생성/검증마다 Base64 디코딩을 반복하지 않기 위함)
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        // Base64 문자열이므로 반드시 디코딩해서 원본 32바이트를 복원
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.secret());

        // 복원한 바이트로 HMAC 서명용 SecretKey 객체 생성
        this.secretKey = Keys.hmacShaKeyFor(keyBytes); // 키 길이 부족 시 이 시점에 예외 발생
    }

    // 액세스 토큰 발급(5분)
    public String createAccessToken(Authentication authentication) {
        return createToken(extractMemberId(authentication), jwtProperties.accessTokenExpiration(), "access");
    }

    // 리프레시 토큰 발급(7일) -> Redis에 해시 저장
    public String createRefreshToken(Authentication authentication) {
        return createToken(extractMemberId(authentication), jwtProperties.refreshTokenExpiration(), "refresh");
    }

    // 인증 객체에서 회원 식별자 추출
    // 인증 주체는 id(PK)
    private String extractMemberId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return userDetails.getUsername();
    }

    // access + refresh 토큰의 공통 생성 로직
    private String createToken(String memberId, long expiration, String category) {

        // 발급 시각
        Date now = new Date();

        // 만료 시각 = 발급 시각 + 유효기간(5분)
        Date expiry = new Date(
                now.getTime() + expiration
        );

        return Jwts.builder()
                .subject(memberId) // 토큰 주체(회원 식별자PK)
                .claim("category", category) // 커스텀 클레임(토큰 구분)
                .issuedAt(now) // iat(발급 시각)
                .expiration(expiry) // exp(만료 시각)
                .signWith(secretKey) // HMAC으로 서명(위조 방지)
                .compact();
    }

    // 토큰 파싱 + 서명 검증을 동시에 수행
    // 검증 실패 시 예외가 발생
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey) // 이 키로 서명을 검증
                .build()
                .parseSignedClaims(token) // 서명이 있는 토큰임을 전제로 파싱
                .getPayload();
    }


    // 토큰에서 회원 ID 추출
    // JWT의 sub는 String 타입 이므로 Long으로 바꿈
    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // 이 토큰이 리프레시 토큰인지 확인
    // 재발급 API 에서 액세스 토큰이 들어오는 것을 차단
    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get("category",String.class));
    }

    // 토큰의 서명과 만료 여부 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 토큰: {}", e.getMessage()); // 정상적으로 발생 가능
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.warn("형식이 잘못된 토큰 접근 시도: {}", e.getMessage()); // 토큰 조작 가능성
        } catch (SignatureException e) {
            log.warn("서명이 유효하지 않은 토큰 접근 시도: {}", e.getMessage()); // 위조 시도 가능성(가장 위험)
        } catch (IllegalArgumentException e) {
            log.debug("빈 토큰 또는 잘못된 인자: {}", e.getMessage()); // 클라이언트 구현 실수 가능성
        }
        return false; // 모든 실패 케이스는 공통으로 false 반환
    }

}
