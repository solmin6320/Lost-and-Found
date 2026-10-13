package com.example.lostandfound.integration;

import com.example.lostandfound.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClaimIntegrationTest extends IntegrationTestSupport {

    private static final String QUESTION = "안에 든 카드 색은?";
    private static final String ANSWER = "파란색 카드 두 장";

    private String finder; // 주운 사람
    private String owner; // 진짜 주인
    private String stranger; // 다른 사람
    private Long postId; // finder가 쓴 습득 글

    @BeforeEach
    void setUp() throws Exception {
        finder = bearerOf("finder@test.com", "주운사람");
        owner = bearerOf("owner@test.com", "주인");
        stranger = bearerOf("stranger@test.com", "지나가는사람");
        postId = writeFoundPost(finder);
    }

    @Test
    @DisplayName("요청 -> 승인 -> 받음 | 글 완료 + 주인 확인 배지 + 되찾기 기록, 답은 상세에 안 실림")
    void fullFlow() throws Exception {

        Long claimId = requestClaim(postId, owner, ANSWER);

        // 비로그인 상세 - 질문만 보이고 답, 요청 정보는 없음
        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationQuestion").value(QUESTION))
                .andExpect(jsonPath("$.answer").doesNotExist())
                .andExpect(jsonPath("$.pendingClaimCount").doesNotExist())
                .andExpect(jsonPath("$.myClaim").doesNotExist());

        // 글쓴이 상세 - 대기 요청 수
        mockMvc.perform(get("/api/posts/{id}", postId).header(AUTHORIZATION, finder))
                .andExpect(jsonPath("$.pendingClaimCount").value(1))
                .andExpect(jsonPath("$.myClaim").doesNotExist());

        // 요청자 상세 - 내 요청
        mockMvc.perform(get("/api/posts/{id}", postId).header(AUTHORIZATION, owner))
                .andExpect(jsonPath("$.myClaim.id").value(claimId))
                .andExpect(jsonPath("$.myClaim.status").value("PENDING"))
                .andExpect(jsonPath("$.pendingClaimCount").doesNotExist());

        // 받은 요청은 글쓴이만
        mockMvc.perform(get("/api/posts/{id}/claims", postId).header(AUTHORIZATION, finder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].answer").value(ANSWER))
                .andExpect(jsonPath("$[0].claimantNickname").value("주인"));

        mockMvc.perform(get("/api/posts/{id}/claims", postId).header(AUTHORIZATION, stranger))
                .andExpect(status().isForbidden());

        // 승인 -> 연락중
        approve(claimId, finder);
        assertThat(postStatus(postId)).isEqualTo("IN_PROGRESS");

        // 승인된 요청이 있으면 글쓴이도 상태를 직접 못 바꿈
        mockMvc.perform(patch("/api/posts/{id}/status", postId).header(AUTHORIZATION, finder)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "DONE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLAIM_IN_PROGRESS"));

        // 습득 글의 받음 처리는 요청자(주인)만
        mockMvc.perform(patch("/api/claims/{id}/receive", claimId).header(AUTHORIZATION, finder))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/claims/{id}/receive", claimId).header(AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        // 완료 + 배지 + 되찾기 기록
        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.ownerVerified").value(true))
                .andExpect(jsonPath("$.returnRecord.requestedAt").isNotEmpty())
                .andExpect(jsonPath("$.returnRecord.approvedAt").isNotEmpty())
                .andExpect(jsonPath("$.returnRecord.receivedAt").isNotEmpty())
                .andExpect(jsonPath("$.returnRecord.nickname").doesNotExist());

        // 목록에도 배지
        mockMvc.perform(get("/api/members/me/posts").header(AUTHORIZATION, finder))
                .andExpect(jsonPath("$.content[0].ownerVerified").value(true));

    }

    @Test
    @DisplayName("분실 글은 요청자(주운사람)가 아니라 글쓴이(주인)가 받음 처리")
    void lostPost_authorReceives() throws Exception {

        // owner가 분실 글을 쓰고 finder가 주웠어요로 요청
        Long lostPostId = writeLostPost(owner);
        Long claimId = requestClaim(lostPostId, finder, "안쪽에 K.S. 라고 적혀 있어요");

        approve(claimId, owner);

        // 요청자는 받음 처리 불가
        mockMvc.perform(patch("/api/claims/{id}/receive", claimId).header(AUTHORIZATION, finder))
                .andExpect(status().isForbidden());

        // 글쓴이가 받음 처리
        mockMvc.perform(patch("/api/claims/{id}/receive", claimId).header(AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        mockMvc.perform(get("/api/posts/{id}", lostPostId))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.ownerVerified").value(true));
    }

    @Test
    @DisplayName("내 글 요청 400 | 같은 글 두 번 409 | 연락중인 글엔 새 요청 409")
    void requestRules() throws Exception {

        mockMvc.perform(claimRequest(postId, finder, "제 거에요"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLAIM_OWN_POST"));

        Long claimId = requestClaim(postId, owner, ANSWER);

        mockMvc.perform(claimRequest(postId, owner, "한 번 더"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CLAIM"));
    }

    @Test
    @DisplayName("승인한 요청을 거절로 되돌리면 글이 다시 게시중")
    void rejectApprovedReopensPost() throws Exception {

        Long claimId = requestClaim(postId, owner, ANSWER);
        approve(claimId, finder);

        mockMvc.perform(patch("/api/claims/{id}/reject", claimId).header(AUTHORIZATION, finder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(postStatus(postId)).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("요청이 달린 글도 지울 수 잇고 요청도 같이 지워짐")
    void deletePostWithClaims() throws Exception {

        requestClaim(postId, owner, ANSWER);
        requestClaim(postId, stranger, "검은 카드요");

        mockMvc.perform(delete("/api/posts/{id}", postId).header(AUTHORIZATION, finder))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM claim", Long.class)).isZero();
    }

    @Test
    @DisplayName("로그인 없이는 요청을 보내거나 받은 요청을 볼 수 없음")
    void requiresLogin() throws Exception {

        mockMvc.perform(post("/api/posts/{id}/claims", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("answer", ANSWER))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/posts/{id}/claims", postId))
                .andExpect(status().isUnauthorized());
    }


    // 확인 질문이 걸린 분실 글
    private Long writeFoundPost(String bearer) throws Exception {

        String body = mockMvc.perform(multipart("/api/posts").header(AUTHORIZATION, bearer)
                .param("type", "LOST")
                .param("title", "갈색 가죽 지갑")
                .param("content", "학생회관 근처에서 잃어버렸어요")
                .param("category", "WALLET")
                .param("location", "학생회관")
                .param("lostFoundDate", LocalDate.now().minusDays(1).toString())
                .param("verificationQuestion", "지갑 안쪽에 적힌 글자는?"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private RequestBuilder claimRequest(Long targetPostId, String token, String answer) {

        return post("/api/posts/{id}/claims", targetPostId).header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("answer", answer)));
    }

    private Long requestClaim(Long targetPostId, String token, String answer) throws Exception {

        String body = mockMvc.perform(claimRequest(targetPostId, token, answer))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void approve(Long claimId, String author) throws Exception {

        mockMvc.perform(patch("/api/claims/{id}/approve", claimId).header(AUTHORIZATION, author))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // 확인 질문이 걸린 분실 글
    private Long writeLostPost(String bearer) throws Exception {

        String body = mockMvc.perform(multipart("/api/posts").header(AUTHORIZATION, bearer)
                        .param("type", "LOST")
                        .param("title", "갈색 가죽 지갑")
                        .param("content", "학생회관 근처에서 잃어버렸어요")
                        .param("category", "WALLET")
                        .param("location", "학생회관")
                        .param("lostFoundDate", LocalDate.now().minusDays(1).toString())
                        .param("verificationQuestion", "지갑 안쪽에 적힌 글자는?"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String postStatus(Long targetPostId) {

        return jdbcTemplate.queryForObject("SELECT status FROM post WHERE post_id = ?", String.class, targetPostId);
    }


}
