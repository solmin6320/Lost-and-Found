package com.example.lostandfound.integration;

import com.example.lostandfound.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 매칭 계산, 후보 조회, 숨김 통합 테스트
public class MatchIntegrationTest extends IntegrationTestSupport {

    private String finder; // 주운 사람
    private String owner; // 잃어버린 사람

    @BeforeEach
    void setUp() throws Exception {
        finder = bearerOf("finder@test.com", "주운사람");
        owner = bearerOf("owner@test.com", "주인");
    }

    @Test
    @DisplayName("분실 글 뒤에 습득 글이 올라오면 두 글이 후보가 되고 분실 글 주인에게 알림")
    void foundPostSuggestsToLostOwner() throws Exception {
        LocalDate today = LocalDate.now();

        Long lostId = write(owner, "LOST", "ETC", "파란 우산 잃어버렸어요", "도서관 2층", today.minusDays(2));
        Long foundId = write(finder, "FOUND", "ETC", "파란 우산", "도서관", today.minusDays(1));

        awaitMatches(1);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> countOf("SELECT COUNT(*) FROM notification WHERE type = 'MATCH_SUGGESTED'") == 1);

        mockMvc.perform(get("/api/notifications")
                        .header(AUTHORIZATION, owner))
                .andExpect(jsonPath("$.content[0].type").value("MATCH_SUGGESTED"))
                .andExpect(jsonPath("$.content[0].postId").value(lostId));


        mockMvc.perform(get("/api/posts/{id}/matches", lostId)
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].postId").value(foundId))
                .andExpect(jsonPath("$[0].type").value("FOUND"))
                .andExpect(jsonPath("$[0].daysApart").value(1))
                .andExpect(jsonPath("$[0].similarPlace").value(true))
                .andExpect(jsonPath("$[0].similarText").value(true));


        // 습득 글 쪽에서도 같은 쌍이 보임
        mockMvc.perform(get("/api/posts/{id}/matches", foundId)
                        .header(AUTHORIZATION, finder))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].postId").value(lostId));
    }

    @Test
    @DisplayName("다른 종류, 날짜 범위 밖, 게시중 아님, 내 글은 후보가 아님")
    void onlyMatchingConditionsBecomeCandidates() throws Exception {
        LocalDate lostDate = LocalDate.now().minusDays(20);

        Long validId = write(finder, "FOUND", "ETC", "검은 지갑", "강남역", lostDate.plusDays(10));
        write(finder, "FOUND", "WALLET", "검은 지갑", "강남역", lostDate.plusDays(1)); // 다른 종류
        write(finder, "FOUND", "ETC", "검은 지갑", "강남역", lostDate.minusDays(1)); // 분실일 전에 주움
        write(finder, "FOUND", "ETC", "검은 지갑", "강남역", lostDate.plusDays(15)); // 14일 넘음
        Long closedId = write(finder, "FOUND", "ETC", "검은 지갑", "강남역", lostDate.plusDays(1));
        changeStatus(finder, closedId, "IN_PROGRESS"); // 게시중 아님
        write(owner, "FOUND", "ETC", "검은 지갑", "강남역", lostDate.plusDays(1)); // 내 글

        Long lostId = write(owner, "LOST", "ETC", "검은 지갑", "강남역", lostDate);

        awaitMatches(1);

        mockMvc.perform(get("/api/posts/{id}/matches", lostId)
                        .header(AUTHORIZATION, owner))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].postId").value(validId));
    }

    @Test
    @DisplayName("내 물건 아니에요 -> 내 목록에서만 빠지고 상대 목록에는 남음")
    void dismissHidesOnlyMySide() throws Exception {
        LocalDate today = LocalDate.now();

        Long lostId = write(owner, "LOST", "ETC", "파란 우산", "도서관", today.minusDays(2));
        Long foundId = write(finder, "FOUND", "ETC", "파란 우산", "도서관", today.minusDays(1));

        awaitMatches(1);

        mockMvc.perform(patch("/api/posts/{id}/matches/{candidateId}/dismiss", lostId, foundId)
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/{id}/matches", lostId)
                        .header(AUTHORIZATION, owner))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/posts/{id}/matches", foundId)
                        .header(AUTHORIZATION, finder))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("남의 글 후보는 403, 없는 후보 숨김은 404, 비로그인은 401")
    void accessRules() throws Exception {

        LocalDate today = LocalDate.now();

        Long lostId = write(owner, "LOST", "ETC", "파란 우산", "도서관", today.minusDays(2));
        Long foundId = write(finder, "FOUND", "WALLET", "파란 우산", "도서관", today.minusDays(1)); // 후보 아님

        mockMvc.perform(get("/api/posts/{id}/matches", lostId)
                        .header(AUTHORIZATION, finder))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCESS"));

        mockMvc.perform(patch("/api/posts/{id}/matches/{candidateId}/dismiss", lostId, foundId)
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));

        mockMvc.perform(get("/api/posts/{id}/matches", lostId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/posts/{id}/matches/{candidateId}/dismiss", lostId, foundId))
                .andExpect(status().isUnauthorized());
    }


    private Long write(String bearer, String type, String category, String title, String location, LocalDate date) throws Exception {
        String body = mockMvc.perform(multipart("/api/posts").header(AUTHORIZATION, bearer)
                        .param("type", type)
                        .param("title", title)
                        .param("content", "본문입니다")
                        .param("category", category)
                        .param("location", location)
                        .param("lostFoundDate", date.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void changeStatus(String bearer, Long postId, String newStatus) throws Exception {
        mockMvc.perform(patch("/api/posts/{id}/status", postId).header(AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", newStatus))))
                .andExpect(status().isOk());
    }

    // 비동기 계산이 끝날 때까지 기다린 뒤 개수가 정확한지 확인
    private void awaitMatches(long expected) {
        await().atMost(Duration.ofSeconds(5)).until(() -> countOf("SELECT COUNT(*) FROM post_match") >= expected);

        assertThat(countOf("SELECT COUNT(*) FROM post_match")).isEqualTo(expected);
    }

    private long countOf(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}