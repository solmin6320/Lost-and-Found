package com.example.lostandfound.service;

import com.example.lostandfound.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

// 리프레시 토큰의 Redis 저장, 조회, 삭제를 담당
// 원본 토큰이 아닌 SHA-256 해시 저장
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:"; // 콜론은 Redis의 관례

    private final StringRedisTemplate redisTemplate;

    // TTL을 하드코딩 하지 않고 yml의 토큰만료시간을 재사용
    private final JwtProperties jwtProperties;

    // 리프레시 토큰 저장(재로그인 시 기존 값을 덮어씀)
    public void save(Long id, String refreshToken) {
        // Redis의 SETEX에 해당
        // 저장 + TTL을 한 번의 통신으로 처리
        redisTemplate.opsForValue().set(
                buildKey(id),
                hash(refreshToken),
                Duration.ofMillis(jwtProperties.refreshTokenExpiration()) // ms 단위(7일)
        );
    }

    // 제출된 토큰이 저장된 값과 일치하는지 검증(재발급에 사용)
    public boolean matches(Long id, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(buildKey(id));

        if (stored == null) {
            return false; // 로그아웃 했거나 TTL이 만료된 경우
        }
        // 원본끼리가 아닌 해시끼리 비교
        return stored.equals(hash(refreshToken));
    }

    // 로그아웃 시 삭제
    // 이미 발급된 액세스 토큰은 만료 전 까지 유효
    // 무상태 설계의 트레이드오프
    public void delete(Long id) {
        // Redis의 DEL에 해당
        redisTemplate.delete(buildKey(id));
    }

    // 키 생성을 한곳에 모아 저장, 조회, 삭제가 항상 동일한 키를 사용하도록 보장
    private String buildKey(Long id) {
        return KEY_PREFIX + id;
    }

    // SHA-256 해싱 후 hex 문자열로 반환
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashByte = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            // 바이트 배열을 16진수 문자열로 변환(64자 고정)
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : hashByte) {
                // %02x 2자리 16진수 -> 한 자리면 앞에 0을 채움
                stringBuilder.append(String.format("%02x", b));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM이 지원하므로 실제로 발생하지 않음
            // 하지만 체크 예외라 명시
            throw  new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }
}
