package com.example.lostandfound.service;

import com.example.lostandfound.entity.*;
import com.example.lostandfound.event.NotificationEvent;
import com.example.lostandfound.event.PostCreatedEvent;
import com.example.lostandfound.repository.PostMatchRepository;
import com.example.lostandfound.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;

// 분실, 습득 자동 매칭
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final PostRepository postRepository;
    private final PostMatchRepository postMatchRepository;
    private final MatchScorer matchScorer;
    private final ApplicationEventPublisher eventPublisher;

    // 글 등록이 커밋된 뒤 별도 스레드에서 계산
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostCreated(PostCreatedEvent event) {

        try {
            match(event.postId());
        } catch (RuntimeException e) {
            log.warn("매칭 계산 실패: postId={}", event.postId(), e);
        }

    }


    // 새 글과 반대 유형 글 사이의 새 쌍을 저장
    // 상대 글 주인에게 알림
    void match(Long postId) {
        Post post = postRepository.findById(postId)
                .orElse(null);
        if (post == null || post.getStatus() != PostStatus.OPEN) {
            return;
        }

        boolean isLost = post.getType() == PostType.LOST;
        LocalDate date = post.getLostFoundDate();

        // 분실일 <= 습득일 <= 분실일 + 14일
        List<Post> candidates = postRepository.findMatchCandidates(
                isLost ? PostType.FOUND : PostType.LOST, post.getCategory(),
                post.getMember().getId(),
                isLost ? date : date.minusDays(MatchScorer.MAX_DAYS),
                isLost ? date.plusDays(MatchScorer.MAX_DAYS) : date
        );

        for (Post other : candidates) {
            Post lost = isLost ? post : other;
            Post found = isLost ? other : post;

            // 한 쌍에 한 번만
            if (postMatchRepository.findByLostPostIdAndFoundPostId(
                    lost.getId(), found.getId()).isPresent()) {
                continue;
            }

            postMatchRepository.save(PostMatch.builder()
                            .lostPost(lost)
                            .foundPost(found)
                            .score(matchScorer.score(lost, found))
                    .build());

            eventPublisher.publishEvent(new NotificationEvent(
                    other.getMember().getId(),

                    NotificationType.MATCH_SUGGESTED,
                    other.getId(),
                    null,
                    other.getTitle(),
                    null
            ));
        }
    }
}
