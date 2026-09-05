package com.example.lostandfound.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
// 조회수 중복 집계 방지
public class PostViewService {

    private static final String KEY_PREFIX = "view:";
    private static final Duration TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    // 키가 없을 때만 등록하고 true
    public boolean isFirstView(Long postId, Long memberId) {

        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(buildKey(postId, memberId), "1", TTL);

        return Boolean.TRUE.equals(isNew);
    }

    private String buildKey(Long postId, Long memberId) {
        return KEY_PREFIX + postId + ":" + memberId;
    }
}
