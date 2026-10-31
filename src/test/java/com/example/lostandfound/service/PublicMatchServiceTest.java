package com.example.lostandfound.service;

import com.example.lostandfound.dto.response.PublicMatchResponse;
import com.example.lostandfound.entity.*;
import com.example.lostandfound.event.NotificationEvent;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.NotificationRepository;
import com.example.lostandfound.repository.PostRepository;
import com.example.lostandfound.repository.PublicFoundItemRepository;
import com.example.lostandfound.repository.PublicMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// 공공 습득물 대조, 후보 조회, 숨김 단위 테스트
@ExtendWith(MockitoExtension.class)
class PublicMatchServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PublicFoundItemRepository publicFoundItemRepository;

    @Mock
    private PublicMatchRepository publicMatchRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final LocalDate lostDate = LocalDate.of(2026, 10, 1);

    private PublicMatchService publicMatchService;

    private Member owner; // 잃어버린 사람

    @BeforeEach
    void setUp() {
        publicMatchService = new PublicMatchService(postRepository, publicFoundItemRepository, publicMatchRepository,
                new PublicMatchScorer(), notificationRepository, eventPublisher);
        owner = memberOf(1L, "owner@test.com", "주인");
    }

    @Test
    @DisplayName("분실 글은 분실일부터 14일 뒤까지 습득된 공공 습득물과 대조하고 주인에게 알림")
    void matchPost_savesPairAndNotifiesOwner() {
        Post lost = postOf(10L, owner, PostType.LOST);
        PublicFoundItem item = itemOf(100L, lostDate.plusDays(2));

        given(postRepository.findById(10L)).willReturn(Optional.of(lost));
        given(publicFoundItemRepository.findByCategoryAndFoundDateBetween(PostCategory.ETC, lostDate, lostDate.plusDays(14)))
                .willReturn(List.of(item));

        publicMatchService.matchPost(10L);

        ArgumentCaptor<PublicMatch> matchCaptor = ArgumentCaptor.forClass(PublicMatch.class);
        verify(publicMatchRepository).save(matchCaptor.capture());

        assertThat(matchCaptor.getValue().getLostPost()).isSameAs(lost);
        assertThat(matchCaptor.getValue().getPublicItem()).isSameAs(item);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationEvent event = eventCaptor.getValue();

        assertThat(event.receiveId()).isEqualTo(1L);
        assertThat(event.type()).isEqualTo(NotificationType.PUBLIC_MATCH_SUGGESTED);
        assertThat(event.postId()).isEqualTo(10L);
        assertThat(event.postTitle()).isEqualTo("파란 우산");
    }

    @Test
    @DisplayName("습득 글이나 게시중이 아닌 분실 글은 대조하지 않음")
    void matchPost_skipsFoundOrClosed() {
        Post found = postOf(20L, owner, PostType.FOUND);
        Post closed = postOf(10L, owner, PostType.LOST);
        closed.changeStatus(PostStatus.IN_PROGRESS);

        given(postRepository.findById(20L)).willReturn(Optional.of(found));
        given(postRepository.findById(10L)).willReturn(Optional.of(closed));

        publicMatchService.matchPost(20L);
        publicMatchService.matchPost(10L);

        verifyNoInteractions(publicFoundItemRepository, publicMatchRepository, eventPublisher);
    }

    @Test
    @DisplayName("게시중 분실 글이 5개를 넘으면 대조하지 않음")
    void matchPost_skipsWhenTooManyOpenLostPosts() {
        Post lost = postOf(10L, owner, PostType.LOST);

        given(postRepository.findById(10L)).willReturn(Optional.of(lost));
        given(postRepository.countOpenPosts(1L, PostType.LOST)).willReturn(6L);

        publicMatchService.matchPost(10L);

        verifyNoInteractions(publicFoundItemRepository, publicMatchRepository, eventPublisher);
    }

    @Test
    @DisplayName("이미 있는 쌍은 다시 저장하지도 알리지도 않음")
    void matchPost_skipsExistingPair() {
        Post lost = postOf(10L, owner, PostType.LOST);

        given(postRepository.findById(10L)).willReturn(Optional.of(lost));
        given(publicFoundItemRepository.findByCategoryAndFoundDateBetween(any(), any(), any()))
                .willReturn(List.of(itemOf(100L, lostDate)));
        given(publicMatchRepository.existsByLostPostIdAndPublicItemId(10L, 100L)).willReturn(true);

        publicMatchService.matchPost(10L);

        verify(publicMatchRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("수집된 습득물은 습득일 14일 전부터 습득일까지의 게시중 분실 글과 대조")
    void matchItems_searchesLostBefore() {
        LocalDate foundDate = lostDate.plusDays(3);
        Post lost = postOf(10L, owner, PostType.LOST);

        given(postRepository.findOpenLostPosts(PostCategory.ETC, foundDate.minusDays(14), foundDate))
                .willReturn(List.of(lost));

        publicMatchService.matchItems(List.of(itemOf(100L, foundDate)));

        verify(publicMatchRepository).save(any());
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("수집 뒤 대조에서도 게시중 분실 글 5개 초과인 사람은 빼고, 사람마다 한 번만 셈")
    void matchItems_countsOpenPostsOncePerMember() {
        Post lost1 = postOf(10L, owner, PostType.LOST);
        Post lost2 = postOf(11L, owner, PostType.LOST);

        given(postRepository.findOpenLostPosts(any(), any(), any())).willReturn(List.of(lost1, lost2));
        given(postRepository.countOpenPosts(1L, PostType.LOST)).willReturn(6L);

        publicMatchService.matchItems(List.of(itemOf(100L, lostDate)));

        verify(postRepository, times(1)).countOpenPosts(1L, PostType.LOST);
        verifyNoInteractions(publicMatchRepository, eventPublisher);
    }

    @Test
    @DisplayName("새로 들어온 습득물이 없으면 아무것도 하지 않음")
    void matchItems_emptyDoesNothing() {
        publicMatchService.matchItems(List.of());

        verifyNoInteractions(postRepository, publicMatchRepository, eventPublisher);
    }

    @Test
    @DisplayName("오늘 일반 + 공공 매칭 알림을 10개 받았으면 쌍만 저장하고 알리지 않음")
    void matchItems_skipsAlertOverCombinedLimit() {
        given(postRepository.findOpenLostPosts(any(), any(), any())).willReturn(List.of(postOf(10L, owner, PostType.LOST)));
        given(notificationRepository.countByReceiverIdAndTypeInAndCreatedAtAfter(
                eq(1L), eq(List.of(NotificationType.MATCH_SUGGESTED, NotificationType.PUBLIC_MATCH_SUGGESTED)), any()))
                .willReturn(10L);

        publicMatchService.matchItems(List.of(itemOf(100L, lostDate)));

        verify(publicMatchRepository).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("같은 대조 안에서 보낸 알림도 세어 하루 10개에서 멈춤")
    void matchItems_countsAlertsInSameRun() {
        given(postRepository.findOpenLostPosts(any(), any(), any())).willReturn(List.of(postOf(10L, owner, PostType.LOST)));
        given(notificationRepository.countByReceiverIdAndTypeInAndCreatedAtAfter(eq(1L), any(), any()))
                .willReturn(9L);

        publicMatchService.matchItems(List.of(itemOf(100L, lostDate), itemOf(101L, lostDate)));

        verify(publicMatchRepository, times(2)).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("남의 글 후보는 볼 수 없고, 습득 글은 빈 목록")
    void getPublicMatches_rules() {
        given(postRepository.findById(10L)).willReturn(Optional.of(postOf(10L, owner, PostType.LOST)));
        given(postRepository.findById(20L)).willReturn(Optional.of(postOf(20L, owner, PostType.FOUND)));

        assertThatThrownBy(() -> publicMatchService.getPublicMatches(10L, 2L))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);

        assertThat(publicMatchService.getPublicMatches(20L, 1L)).isEmpty();
        verifyNoInteractions(publicMatchRepository);
    }

    @Test
    @DisplayName("후보에 날짜 차이와 이름 비슷 여부를 함께 줌")
    void getPublicMatches_returnsReasons() {
        Post lost = postOf(10L, owner, PostType.LOST);
        PublicFoundItem item = itemOf(100L, lostDate.plusDays(2));

        given(postRepository.findById(10L)).willReturn(Optional.of(lost));
        given(publicMatchRepository.findCandidates(10L)).willReturn(List.of(matchOf(lost, item)));

        List<PublicMatchResponse> result = publicMatchService.getPublicMatches(10L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemId()).isEqualTo(100L);
        assertThat(result.get(0).source()).isEqualTo(PublicFoundSource.POLICE);
        assertThat(result.get(0).daysApart()).isEqualTo(2);
        assertThat(result.get(0).similarName()).isTrue();
    }

    @Test
    @DisplayName("후보를 숨기면 숨김 표시, 없는 후보는 404")
    void dismiss() {
        Post lost = postOf(10L, owner, PostType.LOST);
        PublicMatch match = matchOf(lost, itemOf(100L, lostDate));

        given(postRepository.findById(10L)).willReturn(Optional.of(lost));
        given(publicMatchRepository.findByLostPostIdAndPublicItemId(10L, 100L)).willReturn(Optional.of(match));
        given(publicMatchRepository.findByLostPostIdAndPublicItemId(10L, 999L)).willReturn(Optional.empty());

        publicMatchService.dismiss(10L, 100L, 1L);
        assertThat(match.isDismissed()).isTrue();

        assertThatThrownBy(() -> publicMatchService.dismiss(10L, 999L, 1L))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PUBLIC_MATCH_NOT_FOUND);
    }


    private Member memberOf(Long id, String email, String nickname) {
        Member member = Member.builder()
                .email(email)
                .password("encoded")
                .nickname(nickname)
                .build();

        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Post postOf(Long id, Member member, PostType type) {
        Post post = Post.builder()
                .member(member)
                .type(type)
                .title("파란 우산")
                .content("손잡이가 검은색")
                .category(PostCategory.ETC)
                .location("도서관")
                .lostFoundDate(lostDate)
                .build();

        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    // 물품명이 내 글 제목과 같은 습득물
    private PublicFoundItem itemOf(Long id, LocalDate foundDate) {
        PublicFoundItem item = PublicFoundItem.builder()
                .source(PublicFoundSource.POLICE)
                .externalId("A" + id + "-1")
                .itemName("파란 우산")
                .category(PostCategory.ETC)
                .foundDate(foundDate)
                .storagePlace("강남경찰서")
                .build();

        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private PublicMatch matchOf(Post lost, PublicFoundItem item) {
        return PublicMatch.builder()
                .lostPost(lost)
                .publicItem(item)
                .score(50)
                .build();
    }
}