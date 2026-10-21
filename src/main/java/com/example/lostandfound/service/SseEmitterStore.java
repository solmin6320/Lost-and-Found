package com.example.lostandfound.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 열린 SSE 연결 보관
@Component
public class SseEmitterStore {

    private final Map<Long, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void add(Long memberId, String connectionId, SseEmitter emitter) {
        emitters.computeIfAbsent(memberId, id -> new ConcurrentHashMap<>()).put(connectionId, emitter);
    }

    // 마지막 연결이 끊기면 회원 칸도 지움
    public void remove(Long memberId, String connectionId) {
        emitters.computeIfPresent(memberId, (id, connections) -> {

            connections.remove(connectionId);
            return connections.isEmpty() ? null : connections;
        });
    }

    public Map<String, SseEmitter> of(Long memberId) {
        return emitters.getOrDefault(memberId, Map.of());
    }

    public Map<Long, Map<String, SseEmitter>> all() {
        return emitters;
    }
}
