package com.example.lostandfound.service;

import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostCategory;
import com.example.lostandfound.entity.PostType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// 매칭 글자 유사도, 정렬 점수 단위 테스트
class MatchScorerTest {

    private final MatchScorer matchScorer = new MatchScorer();

    private final LocalDate lostDate = LocalDate.of(2026, 10, 1);

    @Test
    @DisplayName("띄어쓰기, 기호, 대소문자는 무시하고 같은 글은 1")
    void similarity_ignoresSpacesAndSymbols() {

        assertThat(MatchScorer.similarity("파란 우산", "파란우산")).isEqualTo(1.0);
        assertThat(MatchScorer.similarity("iP hone 15", "IPHONE15")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("겹치는 두 글자가 없거나, 비었거나, 한 글자면 0")
    void similarity_zeroCases() {
        assertThat(MatchScorer.similarity("파란 우산", "검은 지갑")).isZero();
        assertThat(MatchScorer.similarity(null, "파란 우산")).isZero();
        assertThat(MatchScorer.similarity("우", "우산")).isZero();
    }

    @Test
    @DisplayName("일부만 겹치면  0과 1 사이")
    void similarity_partial() {
        assertThat(MatchScorer.similarity("도서관 2층", "도서관")).isCloseTo(2.0 / 3, within(1e-9));
    }

    @Test
    @DisplayName("모두 같고 같은 날이면 100, 날이 멀수록 낮아짐")
    void scorer_closeDateIsHigher() {
        Post lost = postOf(PostType.LOST, lostDate);

        assertThat(matchScorer.score(lost, postOf(PostType.FOUND, lostDate))).isEqualTo(100);

        assertThat(matchScorer.score(lost, postOf(PostType.FOUND, lostDate.plusDays(7)))).isEqualTo(90);

        assertThat(matchScorer.score(lost, postOf(PostType.FOUND, lostDate.plusDays(14)))).isEqualTo(80);
    }


    // 장소, 제목, 본문이 같은 글
    private Post postOf(PostType type, LocalDate date) {
        return Post.builder()
                .type(type)
                .title("파란 우산")
                .content("손잡이가 검은색")
                .category(PostCategory.ETC)
                .location("도서관")
                .lostFoundDate(date)
                .build();
    }
}
