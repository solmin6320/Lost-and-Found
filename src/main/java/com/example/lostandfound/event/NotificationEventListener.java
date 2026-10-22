package com.example.lostandfound.event;

import com.example.lostandfound.entity.Notification;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.NotificationRepository;
import com.example.lostandfound.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final NotificationStreamService notificationStreamService;

    // 원래 요청이 커밋된 뒤에만 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // 원래 트랜잭션이 끝났으니 새 트랜잭션으로 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationEvent event) {
        try {
            Notification saved = notificationRepository.save(Notification.builder()
                            .receiver(memberRepository.getReferenceById(event.receiveId()))
                            .type(event.type())
                            .postId(event.postId())
                            .claimId(event.claimId())
                            .postTitle(event.postTitle())
                            .actorNickname(event.actorNickname())
                            .build());

            notificationStreamService.send(saved);
        } catch (RuntimeException e) {
            log.warn("알림 저장, 전송 실패: type={}, receiverId={}", event.type(), event.receiveId(), e);
        }
    }
}
