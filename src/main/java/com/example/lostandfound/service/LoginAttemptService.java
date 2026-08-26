package com.example.lostandfound.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Service
@RequiredArgsConstructor
// 로그인 실패 카운트 전담(실제 로그인 로직이 아님)
public class LoginAttemptService {

    private static final String KEY_PREFIX = "login:fail:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    // 현재 잠금 상태인지 확인(인증 시도 전에 먼저 호출)
    public boolean locked(String email) {
        String count = redisTemplate.opsForValue().get(buildKey(email));

        // 로그인 실패 카운트 5회 이상
        return count != null && Integer.parseInt(count) >= MAX_ATTEMPTS;
    }

    // 로그인 실패 시 카운트 증가
    public void recordFailure(String email) {
        String key = buildKey(email);
        
        // INCR은 키가 없으면 0에서 시작해서 1을 반환(기존 TTL 유지)
        Long count = redisTemplate.opsForValue().increment(key);

        // 1회: 카운트 집계(30분) 시작
        // 5회: 잠금 시작 시점부터 다시 30분을 보장
        if (count != null && (count == 1L || count == MAX_ATTEMPTS)) {
            redisTemplate.expire(key, LOCK_DURATION);
        }
    }

    // 로그인 성공 시 카운트 삭제
    public void reset(String email) {
        redisTemplate.delete(buildKey(email));
    }

    private String buildKey(String email) {
        return KEY_PREFIX + email;
    }
}
