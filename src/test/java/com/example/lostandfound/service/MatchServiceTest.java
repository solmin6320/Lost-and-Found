package com.example.lostandfound.service;


import com.example.lostandfound.config.AwsProperties;
import com.example.lostandfound.dto.response.MatchResponse;
import com.example.lostandfound.entity.*;
import com.example.lostandfound.event.NotificationEvent;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.NotificationRepository;
import com.example.lostandfound.repository.PostImageRepository;
import com.example.lostandfound.repository.PostMatchRepository;
import com.example.lostandfound.repository.PostRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// 매칭 계산, 후보 조회, 숨김 단위 테스트
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMatchRepository postMatchRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private NotificationRepository notificationRepository;

    // 설정값은 목이 아니라 실제 객체
    private final AwsProperties awsProperties = new AwsProperties(
            "ap-northeast-2",
            new AwsProperties.S3("test-bucket", "https://cdn.test"),
            new AwsProperties.Credentials("test-key", "test-secret")
    );

    private MatchService matchService;

    private final LocalDate lostDate = LocalDate.of(2026, 10, 1);

    private Member owner; // 잃어버린 사람
    private Member finder; // 주운 사람

    @BeforeEach
    void setUp() {
        matchService = new MatchService(postRepository, postMatchRepository, new MatchScorer(), eventPublisher,
                postImageRepository, awsProperties, notificationRepository);
        owner = memberOf(1L, "owner@test.com", "주인");
        finder = memberOf(2L, "finder@test.com", "주운사람");
    }

    @Test
    @DisplayName("분실 글은 분실일부터 14일 뒤까지의 습득 글을 찾음")
    void match_lostSearchesFoundAfter() {
        Post lost = postOf(10L, owner, PostType.LOST, lostDate);

        given(postRepository.findById(10L)).willReturn(Optional.of(lost));
        given(postRepository.findMatchCandidates(PostType.FOUND, PostCategory.ETC, 1L, lostDate, lostDate.plusDays(14)))
                .willReturn(List.of());

        matchService.match(10L);

        verifyNoInteractions(postMatchRepository, eventPublisher);
    }

    @Test
    @DisplayName("습득 글은 습득일 14일 전부터 습득일까지의 분실 글을 찾음")
    void match_foundSearchesLostBefore() {
        LocalDate foundDate = lostDate.plusDays(3);
        Post found = postOf(20L, finder, PostType.FOUND, foundDate);

        given(postRepository.findById(20L)).willReturn(Optional.of(found));
        given(postRepository.findMatchCandidates(PostType.LOST, PostCategory.ETC, 2L, foundDate.minusDays(14), foundDate))
                .willReturn(List.of());

        matchService.match(20L);

        verifyNoInteractions(postMatchRepository, eventPublisher);
    }

    @Test
    @DisplayName("새 쌍을 저장하고 상대 글 주인에게 매칭 알림")
    void match_savesPairAndNotifiesOtherOwner() {

        Post lost = postOf(10L, owner, PostType.LOST, lostDate);
        Post found = postOf(20L, finder, PostType.FOUND, lostDate.plusDays(1));

        given(postRepository.findById(20L)).willReturn(Optional.of(found));
        given(postRepository.findMatchCandidates(any(), any(), anyLong(), any(), any())).willReturn(List.of(lost));
        given(postMatchRepository.findByLostPostIdAndFoundPostId(10L, 20L)).willReturn(Optional.empty());

        matchService.match(20L);

        ArgumentCaptor<PostMatch> matchCaptor = ArgumentCaptor.forClass(PostMatch.class);

        verify(postMatchRepository).save(matchCaptor.capture());

        assertThat(matchCaptor.getValue().getLostPost()).isSameAs(lost);
        assertThat(matchCaptor.getValue().getFoundPost()).isSameAs(found);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationEvent event = eventCaptor.getValue();

        assertThat(event.receiveId()).isEqualTo(1L);
        assertThat(event.type()).isEqualTo(NotificationType.MATCH_SUGGESTED);
        assertThat(event.postId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("이미 있는 쌍은 다시 저장하지도 알리지도 않음")
    void match_skipsExistingPair() {
        Post lost = postOf(10L, owner, PostType.LOST, lostDate);
        Post found = postOf(20L, finder, PostType.FOUND, lostDate.plusDays(1));

        given(postRepository.findById(20L)).willReturn(Optional.of(found));
        given(postRepository.findMatchCandidates(any(), any(), anyLong(), any(), any())).willReturn(List.of(lost));
        given(postMatchRepository.findByLostPostIdAndFoundPostId(10L, 20L))
                .willReturn(Optional.of(matchOf(lost, found)));

        matchService.match(20L);

        verify(postMatchRepository, never()).save(any());

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("게시중이 아닌 글은 매칭하지 않음")
    void match_skipClosedPost() {
        Post found = postOf(20L, finder, PostType.FOUND, lostDate);

        found.changeStatus(PostStatus.IN_PROGRESS);

        given(postRepository.findById(20L)).willReturn(Optional.of(found));

        matchService.match(20L);
        verify(postRepository, never()).findMatchCandidates(any(), any(), anyLong(), any(), any());

        verifyNoInteractions(postMatchRepository, eventPublisher);
    }

    @Test
    @DisplayName("남의 글 후보는 볼 수 없음")
    void getMatches_othersPost() {
        Post lost = postOf(10L, owner, PostType.LOST, lostDate);

        given(postRepository.findById(10L)).willReturn(Optional.of(lost));

        assertThatThrownBy(() -> matchService.getMatches(10L, 2L))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);

        verifyNoInteractions(postMatchRepository);
    }

    @Test
    @DisplayName("게시중인 후보만 보이고, 날짜 차이와 맞은 이유를 함께 줌")
    void getMatches_onlyOpenWithReasons() {
        Post lost = postOf(10L, owner, PostType.LOST, lostDate);
        Post open = postOf(20L, finder, PostType.FOUND, lostDate.plusDays(3));
        Post closed = postOf(21L, finder, PostType.FOUND, lostDate.plusDays(1));

        closed.changeStatus(PostStatus.IN_PROGRESS);

        given(postRepository.findById(10L)).willReturn(Optional.of(lost));
        given(postMatchRepository.findCandidatesForLost(10L))
                .willReturn(List.of(matchOf(lost, closed), matchOf(lost, open)));

        given(postImageRepository.findThumbnails(List.of(20L))).willReturn(List.of());

        List<MatchResponse> result = matchService.getMatches(10L, 1L);

        assertThat(result).hasSize(1);
        MatchResponse response = result.get(0);

        assertThat(response.postId()).isEqualTo(20L);
        assertThat(response.daysApart()).isEqualTo(3);
        assertThat(response.similarPlace()).isTrue();
        assertThat(response.similarText()).isTrue();
        assertThat(response.thumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("분실 글 주인이 숨기면 분실 쪽만 숨김")
    void dismiss_hidesOnlyMySide() {
        Post lost = postOf(10L, owner, PostType.LOST, lostDate);
        Post found = postOf(20L, finder, PostType.FOUND, lostDate.plusDays(1));
        PostMatch match = matchOf(lost, found);

        given(postRepository.findById(10L)).willReturn(Optional.of(lost));
        given(postMatchRepository.findByLostPostIdAndFoundPostId(10L, 20L)).willReturn(Optional.of(match));

        matchService.dismiss(10L, 20L, 1L);

        assertThat(match.isDismissedByLost()).isTrue();
        assertThat(match.isDismissedByFound()).isFalse();
    }

    @Test
    @DisplayName("습득 글 주인이 없는 후보를 숨기면 404")
    void dismiss_missingMatch() {
        Post found = postOf(20L, finder, PostType.FOUND, lostDate);

        given(postRepository.findById(20L)).willReturn(Optional.of(found));

        // 습득 글 쪽 이면 후보가 분실 글 자리로 감
        given(postMatchRepository.findByLostPostIdAndFoundPostId(99L, 20L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.dismiss(20L, 99L, 2L))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MATCH_NOT_FOUND);
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

    // 종류, 장소, 제목이 같은 글
    private Post postOf(Long id, Member member, PostType type, LocalDate date) {
        Post post = Post.builder()
                .member(member)
                .type(type)
                .title("파란 우산")
                .content("손잡이가 검은색")
                .category(PostCategory.ETC)
                .location("도서관")
                .lostFoundDate(date)
                .build();

        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private PostMatch matchOf(Post lost, Post found) {
        return PostMatch.builder()
                .lostPost(lost)
                .foundPost(found)
                .score(50)
                .build();
    }

}
