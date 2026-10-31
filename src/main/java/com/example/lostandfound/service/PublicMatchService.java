package com.example.lostandfound.service;

import com.example.lostandfound.dto.response.PublicMatchResponse;
import com.example.lostandfound.entity.*;
import com.example.lostandfound.event.NotificationEvent;
import com.example.lostandfound.event.PostCreatedEvent;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.NotificationRepository;
import com.example.lostandfound.repository.PostRepository;
import com.example.lostandfound.repository.PublicFoundItemRepository;
import com.example.lostandfound.repository.PublicMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 분실 글, 공공 습득물 자동 대조
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicMatchService {

    private final PostRepository postRepository;
    private final PublicFoundItemRepository publicFoundItemRepository;
    private final PublicMatchRepository publicMatchRepository;
    private final PublicMatchScorer publicMatchScorer;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 일반 매칭과 같은 상한
    private static final List<NotificationType> MATCH_TYPES = List.of(NotificationType.MATCH_SUGGESTED, NotificationType.PUBLIC_MATCH_SUGGESTED);

    // 분실 글 등록이 커밋된 뒤 별도 스레드에서 대조
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostCreated(PostCreatedEvent event) {

        try {
            matchPost(event.postId());
        } catch (RuntimeException e) {
            log.warn("공공 습득물 대조 실패: postId={}", event.postId(), e);
        }
    }

    // 새 분실 글과 저장된 공공 습득물 대조
    void matchPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElse(null);

        if (post == null || post.getType() != PostType.LOST || post.getStatus() != PostStatus.OPEN) {
            return;
        }

        if (tooManyOpenLostPosts(post.getMember().getId())) {
            return;
        }

        LocalDate date = post.getLostFoundDate();
        List<PublicFoundItem> items = publicFoundItemRepository.findByCategoryAndFoundDateBetween(
                post.getCategory(), date, date.plusDays(MatchScorer.MAX_DAYS)
        );

        Map<Long, Long> alerts = new HashMap<>();

        for (PublicFoundItem item : items) {

            saveAndAlert(post, item, alerts);
        }
    }


    // 수집 뒤 새로 들어온 공공 습득물과 게시중 분실 글 대조
    @Transactional
    public void matchItems(List<PublicFoundItem> items) {

        if (items.isEmpty()) {
            return;
        }

        Map<Long, Long> alerts = new HashMap<>();
        Map<Long, Boolean> overLimit = new HashMap<>(); // 사람마다 한 번만 셈

        for (PublicFoundItem item : items) {
            LocalDate date = item.getFoundDate();

            List<Post> lostPosts = postRepository.findOpenLostPosts(
                    item.getCategory(),
                    date.minusDays(MatchScorer.MAX_DAYS), date
            );

            for (Post lost : lostPosts) {

                if (overLimit.computeIfAbsent(lost.getMember().getId(), this::tooManyOpenLostPosts)) {
                    continue;
                }

                saveAndAlert(lost, item, alerts);
            }
        }
    }

    // 내 분실 글의 공공 습득물 후보
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<PublicMatchResponse> getPublicMatches(Long postId, Long memberId) {
        Post post = findMyPost(postId, memberId);

        if (post.getType() != PostType.LOST) {
            return List.of();
        }

        return publicMatchRepository.findCandidates(postId).stream()
                .map(match -> {
                    PublicFoundItem item = match.getPublicItem();
                    long days = Math.abs(ChronoUnit.DAYS.between(post.getLostFoundDate(), item.getFoundDate()));

                    return PublicMatchResponse.of(item, days, PublicMatchScorer.similarName(post, item));
                })
                .toList();
    }

    // 공공 습득물 후보 숨김
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void dismiss(Long postId, Long itemId, Long memberId) {

        findMyPost(postId, memberId);

        PublicMatch match = publicMatchRepository.findByLostPostIdAndPublicItemId(postId, itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.PUBLIC_MATCH_NOT_FOUND));

        match.dismiss();
    }

    // 내 글인지 확인
    private Post findMyPost(Long postId, Long memberId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return post;
    }

    // 새 쌍이면 저장하고 분실 글 주인에게 알림
    private void saveAndAlert(Post lost, PublicFoundItem item, Map<Long, Long> alerts) {

        // 한 쌍에 한 번만
        if (publicMatchRepository.existsByLostPostIdAndPublicItemId(lost.getId(), item.getId())) {
            return;
        }

        publicMatchRepository.save(PublicMatch.builder()
                        .lostPost(lost)
                        .publicItem(item)
                        .score(publicMatchScorer.score(lost, item))
                .build());

        Long receiverId = lost.getMember().getId();

        if (canAlert(receiverId, alerts)) {

            eventPublisher.publishEvent(new NotificationEvent(
                    receiverId,
                    NotificationType.PUBLIC_MATCH_SUGGESTED,
                    lost.getId(),
                    null,
                    lost.getTitle(),
                    null
            ));
        }
    }
    // 분실 글을 많이 걸어 두고 흝는 것을 막음
    private boolean tooManyOpenLostPosts(Long memberId) {
        return postRepository.countOpenPosts(memberId, PostType.LOST) > MatchService.MAX_OPEN_LOST_POSTS;
    }

    // 하루 상한 안이면 알림 보냄
    private boolean canAlert(Long receiverId, Map<Long, Long> alerts) {
        long sentToday = notificationRepository.countByReceiverIdAndTypeInAndCreatedAtAfter(

                receiverId, MATCH_TYPES, LocalDate.now().atStartOfDay());

        long sentNow = alerts.merge(receiverId, 1L, Long::sum); // 이번 알림 포함

        return sentToday + sentNow <= MatchService.MAX_MATCH_ALERTS_PER_DAY;
    }
}
