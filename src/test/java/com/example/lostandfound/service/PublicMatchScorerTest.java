package com.example.lostandfound.service;

import com.example.lostandfound.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 공공 습득물 정렬 점수 단위 테스트
class PublicMatchScorerTest {

    private final PublicMatchScorer scorer = new PublicMatchScorer();

    private final LocalDate lostDate = LocalDate.of(2026, 10, 1);

    @Test
    @DisplayName("글자가 모두 같고 같은 날이면 100, 날이 멀수록 낮아짐")
    void score_closeDateIsHigher() {
        Post lost = lostOf("파란 우산", "파란 우산");

        assertThat(scorer.score(lost, itemOf("파란 우산", null, null, lostDate))).isEqualTo(100);
        assertThat(scorer.score(lost, itemOf("파란 우산", null, null, lostDate.plusDays(7)))).isEqualTo(85);
        assertThat(scorer.score(lost, itemOf("파란 우산", null, null, lostDate.plusDays(14)))).isEqualTo(70);
    }

    @Test
    @DisplayName("글자가 하나도 안 겹치면 날짜 점수만 남음")
    void score_onlyDateWhenNoText() {
        Post lost = lostOf("검은 지갑", "가죽");

        assertThat(scorer.score(lost, itemOf("파란 우산", null, null, lostDate))).isEqualTo(30);
    }

    @Test
    @DisplayName("색상, 게시 제목까지 합쳐 비교하고 비어 있어도 오류 없음")
    void score_usesColorAndTitle() {
        Post lost = lostOf("검정 우산", "본문");

        int withColor = scorer.score(lost, itemOf("우산", "검정", null, lostDate));
        int withoutColor = scorer.score(lost, itemOf("우산", null, null, lostDate));

        assertThat(withColor).isGreaterThan(withoutColor);
    }

    @Test
    @DisplayName("내 제목과 물품명이 비슷하면 이름 비슷")
    void similarName() {
        assertThat(PublicMatchScorer.similarName(lostOf("파란 우산", "본문"), itemOf("파란 우산", "파랑", "파란 우산 습득", lostDate))).isTrue();
        assertThat(PublicMatchScorer.similarName(lostOf("검은 지갑", "본문"), itemOf("파란 우산", null, null, lostDate))).isFalse();
    }

    private Post lostOf(String title, String content) {
        return Post.builder()
                .type(PostType.LOST)
                .title(title)
                .content(content)
                .category(PostCategory.ETC)
                .location("도서관")
                .lostFoundDate(lostDate)
                .build();
    }

    private PublicFoundItem itemOf(String itemName, String colorName, String title, LocalDate foundDate) {
        return PublicFoundItem.builder()
                .source(PublicFoundSource.POLICE)
                .externalId("A1-1")
                .itemName(itemName)
                .category(PostCategory.ETC)
                .colorName(colorName)
                .foundDate(foundDate)
                .title(title)
                .build();
    }
}