package com.example.lostandfound.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

// SSE 연결 보관 단위 테스트
class SseEmitterStoreTest {

    private final SseEmitterStore store = new SseEmitterStore();

    @Test
    @DisplayName("한 회원이 탭 여러 개로 연결하면 각각 보관")
    void keepsEveryConnection() {
        store.add(1L, "tab-a", new SseEmitter());
        store.add(1L, "tab-b", new SseEmitter());

        assertThat(store.of(1L)).hasSize(2);

    }

    @Test
    @DisplayName("마지막 연결이 끊기면 회원 칸도 지워짐")
    void removesMemberWhenLastConnectionCloses() {
        store.add(1L, "tab-a", new SseEmitter());
        store.add(1L, "tab-b", new SseEmitter());
        store.remove(1L, "tab-a");

        assertThat(store.of(1L)).hasSize(1);

        store.remove(1L, "tab-b");

        assertThat(store.all()).doesNotContainKey(1L);
        assertThat(store.of(1L)).isEmpty();
    }
}
