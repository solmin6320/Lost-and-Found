package com.example.lostandfound.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

// 조회수 중복 집계 방지 단위 테스트
@ExtendWith(MockitoExtension.class)
public class PostViewServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PostViewService postViewService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("키가 이미 있으면 중복 조회로 판정")
    void isFirstView_keyExists_returnFalse() {

        given(valueOperations.setIfAbsent(anyString(), eq("1"),
                any(Duration.class))).willReturn(false);

        boolean result = postViewService.isFirstView(3L, 7L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Redis가 null을 돌려줘도 NPE 없이 false로 판정")
    void isFirstView_nullResult_returnFalse() {

        given(valueOperations.setIfAbsent(anyString(), eq("1"),
                any(Duration.class)))
                .willReturn(null);

        boolean result = postViewService.isFirstView(3L, 7L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("키는 view:{postId}:{memberId} 형식이고 TTL은 1일")
    void isFirstView_keyFormatAndTtl() {

        given(valueOperations.setIfAbsent(anyString(), eq("1"),
                any(Duration.class)))
                .willReturn(true);

        postViewService.isFirstView(3L, 7L);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("1"), ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("view:3:7");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofDays(1));
    }
}
