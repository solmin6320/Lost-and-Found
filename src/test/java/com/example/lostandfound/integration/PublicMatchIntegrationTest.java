package com.example.lostandfound.integration;

import com.example.lostandfound.client.PublicFoundPage;
import com.example.lostandfound.entity.PublicFoundItem;
import com.example.lostandfound.entity.PublicFoundSource;
import com.example.lostandfound.service.PublicFoundSaver;
import com.example.lostandfound.service.PublicMatchService;
import com.example.lostandfound.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 공공 습득물 대조, 후보 조회, 숨김 통합 테스트(외부 API는 부르지 않음)
public class PublicMatchIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private PublicFoundSaver publicFoundSaver;

    @Autowired
    private PublicMatchService publicMatchService;

    private String owner; // 잃어버린 사람
    private String other; // 다른 사람

    @BeforeEach
    void setUp() throws Exception {
        owner = bearerOf("owner@test.com", "주인");
        other = bearerOf("other@test.com", "다른사람");
    }

    @Test
    @DisplayName("분실 글을 올리면 조건이 맞는 공공 습득물만 후보가 되고 주인에게 알림")
    void lostPostMatchesSavedItems() throws Exception {
        LocalDate today = LocalDate.now();

        insertItem("A1", "ETC", today.minusDays(1));
        insertItem("A2", "WALLET", today.minusDays(1)); // 다른 종류
        insertItem("A3", "ETC", today.minusDays(5)); // 분실일 전에 습득

        Long lostId = writeLost(owner, today.minusDays(3));

        awaitPublicMatches(1);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> countOf("SELECT COUNT(*) FROM notification WHERE type = 'PUBLIC_MATCH_SUGGESTED'") == 1);

        mockMvc.perform(get("/api/posts/{id}/public-matches", lostId)
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].itemId").value(itemIdOf("A1")))
                .andExpect(jsonPath("$[0].source").value("POLICE"))
                .andExpect(jsonPath("$[0].itemName").value("파란 우산"))
                .andExpect(jsonPath("$[0].storagePlace").value("강남경찰서"))
                .andExpect(jsonPath("$[0].daysApart").value(2))
                .andExpect(jsonPath("$[0].similarName").value(true));
    }

    @Test
    @DisplayName("수집 뒤 새로 들어온 습득물만 게시중 분실 글과 대조하고, 다시 받은 습득물은 대조하지 않음")
    void collectedItemsMatchOpenLostPosts() throws Exception {
        LocalDate today = LocalDate.now();

        writeLost(owner, today.minusDays(3));

        // 등록 뒤 비동기 대조(저장된 습득물 없음)가 먼저 끝나게
        await().pollDelay(Duration.ofMillis(300)).until(() -> true);

        List<PublicFoundPage.Row> rows = List.of(rowOf("P1", today.minusDays(1)));

        List<PublicFoundItem> added = publicFoundSaver.savePage(PublicFoundSource.PORTAL, rows);
        publicMatchService.matchItems(added);

        awaitPublicMatches(1);
        await().atMost(Duration.ofSeconds(5))
                .until(() -> countOf("SELECT COUNT(*) FROM notification WHERE type = 'PUBLIC_MATCH_SUGGESTED'") == 1);

        // 같은 습득물을 다시 받으면 갱신만 되고 새로 추가되지 않음
        List<PublicFoundItem> again = publicFoundSaver.savePage(PublicFoundSource.PORTAL, rows);
        publicMatchService.matchItems(again);

        assertThat(again).isEmpty();
        assertThat(countOf("SELECT COUNT(*) FROM public_found_item")).isEqualTo(1);
        assertThat(countOf("SELECT COUNT(*) FROM public_match")).isEqualTo(1);
    }

    @Test
    @DisplayName("습득 글은 빈 목록, 남의 글은 403, 숨기면 빠지고, 없는 후보는 404, 비로그인은 401")
    void accessRules() throws Exception {
        LocalDate today = LocalDate.now();

        insertItem("A1", "ETC", today.minusDays(1));

        Long lostId = writeLost(owner, today.minusDays(3));
        Long foundId = writePost(owner);

        awaitPublicMatches(1);
        Long itemId = itemIdOf("A1");

        mockMvc.perform(get("/api/posts/{id}/public-matches", foundId)
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/posts/{id}/public-matches", lostId)
                        .header(AUTHORIZATION, other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCESS"));

        mockMvc.perform(patch("/api/posts/{id}/public-matches/{itemId}/dismiss", lostId, itemId)
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/{id}/public-matches", lostId)
                        .header(AUTHORIZATION, owner))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/posts/{id}/public-matches/{itemId}/dismiss", lostId, 999999L)
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PUBLIC_MATCH_NOT_FOUND"));

        mockMvc.perform(get("/api/posts/{id}/public-matches", lostId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/posts/{id}/public-matches/{itemId}/dismiss", lostId, itemId))
                .andExpect(status().isUnauthorized());
    }


    private Long writeLost(String bearer, LocalDate date) throws Exception {
        String body = mockMvc.perform(multipart("/api/posts").header(AUTHORIZATION, bearer)
                        .param("type", "LOST")
                        .param("title", "파란 우산 잃어버렸어요")
                        .param("content", "본문입니다")
                        .param("category", "ETC")
                        .param("location", "도서관")
                        .param("lostFoundDate", date.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    // 외부 API 대신 받은 것처럼 바로 넣음
    private void insertItem(String externalId, String category, LocalDate foundDate) {
        jdbcTemplate.update("INSERT INTO public_found_item " +
                        "(source, external_id, item_name, category, category_name, color_name, found_date, storage_place, image_url, title, fetched_at) " +
                        "VALUES ('POLICE', ?, '파란 우산', ?, '기타물품 > 기타', '파랑', ?, '강남경찰서', NULL, '파란 우산 습득', NOW())",
                externalId, category, foundDate);
    }

    private PublicFoundPage.Row rowOf(String atcId, LocalDate foundDate) {
        return new PublicFoundPage.Row(atcId, "1", "파란 우산", "기타물품 > 기타", "파랑",
                foundDate, "서울교통공사 유실물센터", null, "파란 우산 습득");
    }

    private Long itemIdOf(String externalId) {
        return jdbcTemplate.queryForObject("SELECT public_item_id FROM public_found_item WHERE external_id = ?", Long.class, externalId);
    }

    // 비동기 대조가 끝날 때까지 기다린 뒤 개수가 정확한지 확인
    private void awaitPublicMatches(long expected) {
        await().atMost(Duration.ofSeconds(5)).until(() -> countOf("SELECT COUNT(*) FROM public_match") >= expected);

        assertThat(countOf("SELECT COUNT(*) FROM public_match")).isEqualTo(expected);
    }

    private long countOf(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}