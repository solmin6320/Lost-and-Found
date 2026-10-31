package com.example.lostandfound.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.example.lostandfound.entity.PostCategory.*;
import static org.assertj.core.api.Assertions.assertThat;

// 공공 습득물 분류명 매핑 단위 테스트
class PublicCategoryMapperTest {

    private final PublicCategoryMapper mapper = new PublicCategoryMapper();

    @Test
    @DisplayName("대분류 이름으로 우리 카테고리를 정함")
    void map_byTopCategory() {
        assertThat(mapper.map("지갑 > 남성용 지갑")).isEqualTo(WALLET);
        assertThat(mapper.map("휴대폰 > 스마트폰")).isEqualTo(ELECTRONICS);
        assertThat(mapper.map("컴퓨터 > 노트북")).isEqualTo(ELECTRONICS);
        assertThat(mapper.map("카드 > 신용(체크)카드")).isEqualTo(CARD);
        assertThat(mapper.map("의류 > 여성의류")).isEqualTo(CLOTHES);
    }

    @Test
    @DisplayName("앞뒤 공백이 있거나 소분류가 없어도 대분류로 맞춤")
    void map_stripsAndWorksWithoutSub() {
        assertThat(mapper.map("  지갑  > 기타")).isEqualTo(WALLET);
        assertThat(mapper.map("카드")).isEqualTo(CARD);
    }

    @Test
    @DisplayName("모르는 이름과 null은 ETC")
    void map_unknownIsEtc() {
        assertThat(mapper.map("가방 > 여성용가방")).isEqualTo(ETC);
        assertThat(mapper.map(null)).isEqualTo(ETC);
    }
}