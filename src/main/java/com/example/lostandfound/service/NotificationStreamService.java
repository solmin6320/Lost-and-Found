package com.example.lostandfound.service;

import com.example.lostandfound.dto.response.NotificationResponse;
import com.example.lostandfound.entity.Notification;
import com.example.lostandfound.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

// 실시간 알림 연결(SSE)
// 구독, 전송, heartbeat
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationStreamService {

    private final SseEmitterStore store;
    private final NotificationRepository notificationRepository;

    // 연결 유지 시간
    private static final long TIMEOUT_MS = 30L * 60 * 1000;

    // 구독 - 연결 직후 한 번 보냄
    // 놓친 알림은 다시 보냄
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public SseEmitter subscribe(Long memberId, Long lastEventId) {

        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        String connectionId = UUID.randomUUID().toString();

        store.add(memberId, connectionId, emitter);
        emitter.onCompletion(() -> store.remove(memberId, connectionId));
        emitter.onTimeout(() -> store.remove(memberId, connectionId));
        emitter.onError(e -> store.remove(memberId, connectionId));

        // 응답이 바로 시작되게
        sendTo(emitter, memberId, connectionId, SseEmitter.event().name("connected").data("ok"));

        if (lastEventId != null) {
            notificationRepository.findTop50ByReceiverIdAndIdGreaterThanOrderByIdAsc(memberId, lastEventId)
                    .forEach(n -> sendTo(emitter, memberId, connectionId, toEvent(n)));
        }
        return emitter;
    }

    // 알림 하나를 받는 사람의 모든 연결로 보냄
    public void send(Notification notification) {
        Long memberId = notification.getReceiver().getId();

        store.of(memberId).forEach((connectionId, emitter) -> sendTo(emitter, memberId, connectionId, toEvent(notification)));
    }

    // 15초마다 주설 줄
    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        store.all().forEach((memberId, connections) -> connections.forEach((connectionId, emitter) ->
                sendTo(emitter, memberId, connectionId, SseEmitter.event().comment("heartbeat"))));
    }



    private SseEmitter.SseEventBuilder toEvent(Notification notification) {

        return SseEmitter.event()
                .id(String.valueOf(notification.getId()))
                .name("notification")
                .data(NotificationResponse.from(notification), MediaType.APPLICATION_JSON);
    }

    // 끊긴 연결은 보내다 실패하면 지움
    private void sendTo(SseEmitter emitter, Long memberId, String connectionId, SseEmitter.SseEventBuilder event) {

        try {
            emitter.send(event);
        } catch (IOException | IllegalArgumentException e) {
            store.remove(memberId, connectionId);
            log.debug("SSE 전송 실패로 연결 정리: memberId={}", memberId);
        }
    }
}
