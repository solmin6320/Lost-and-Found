package com.example.lostandfound.integration;

import com.example.lostandfound.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NotificationIntegrationTest extends IntegrationTestSupport {

    private String finder; // 주운 사람(글쓴이)
    private String owner; // 요청하는 사람
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        finder = bearerOf("finder@test.com", "주운사람");
        owner = bearerOf("owner@test.com", "주인");
        postId = writePost(finder);
    }

    @Test
    @DisplayName("확인 요청 -> 글쓴이에게 -> 승인 -> 요청한 사람에게 알림")
    void claimFlowCreatesNotifications() throws Exception {
        Long claimId = requestClaim(owner);

        mockMvc.perform(get("/api/notifications")
                .header(AUTHORIZATION, finder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("CLAIM_REQUESTED"))
                .andExpect(jsonPath("$.content[0].postId").value(postId))
                .andExpect(jsonPath("$.content[0].claimId").value(claimId))
                .andExpect(jsonPath("$.content[0].postTitle").value("파란 우산"))
                .andExpect(jsonPath("$.content[0].actorNickname").value("주인"))
                .andExpect(jsonPath("$.content[0].read").value(false));

        mockMvc.perform(patch("/api/claims/{id}/approve", claimId)
                .header(AUTHORIZATION, finder))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                .header(AUTHORIZATION, owner))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("CLAIM_APPROVED"))
                .andExpect(jsonPath("$.content[0].actorNickname").value("주운사람"));
    }

    @Test
    @DisplayName("거절되면 요청한 사람에게 CLAIM_REJECTED")
    void rejectCreateNotification() throws Exception {

        Long claimId = requestClaim(owner);

        mockMvc.perform(patch("/api/claims/{id}/reject", claimId)
                .header(AUTHORIZATION, finder))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                .header(AUTHORIZATION, owner))
                .andExpect(jsonPath("$.content[0].type").value("CLAIM_REJECTED"));
    }

    @Test
    @DisplayName("안 읽은 수 -> 하나 읽음 -> 모두 읽음, 남의 알림은 404")
    void unreadAndMarkRead() throws Exception {
        requestClaim(owner);
        requestClaim(bearerOf("other@test.com", "다른사람"));

        assertThat(unreadCount(finder)).isEqualTo(2);

        Long notificationId = firstNotificationId(finder);

        // 남의 알림은 없는 것과 같게
        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
                .header(AUTHORIZATION, owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));

        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
                .header(AUTHORIZATION, finder))
                .andExpect(status().isNoContent());

        assertThat(unreadCount(finder)).isEqualTo(1);

        mockMvc.perform(patch("/api/notifications/read-all")
                .header(AUTHORIZATION, finder))
                .andExpect(status().isNoContent());

        assertThat(unreadCount(finder)).isZero();
    }

    @Test
    @DisplayName("실시간 연결, 연결 직후 connected, Last-Event-ID 뒤의 놓친 알림을 다시 보냄")
    void streamReplaysMissedNotifications() throws Exception {

        requestClaim(owner);
        Long notificationId = firstNotificationId(finder);

        MvcResult result = mockMvc.perform(get("/api/notifications/stream")
                .header(AUTHORIZATION, finder)
                .header("Last-Event-ID", notificationId -1)
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = result.getResponse().getContentAsString();

        assertThat(body).contains("event:connected");
        assertThat(body).contains("id:" + notificationId);
        assertThat(body).contains("event:notification");
        assertThat(body).contains("CLAIM_REQUESTED");
    }

    @Test
    @DisplayName("비로그인은 알림 API, 실시간 연결 모두 401")
    void anonymousDenied() throws Exception {

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications/stream")
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
    }


    private Long requestClaim(String token) throws Exception {
            String body = mockMvc.perform(post("/api/posts/{id}/claims", postId)
                    .header(AUTHORIZATION, token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("answer", "파란색 손잡이"))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long unreadCount(String token) throws Exception {
        String body = mockMvc.perform(get("/api/notifications/unread-count")
                .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.count")).longValue();
    }

    private Long firstNotificationId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/notifications")
                .header(AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.content[0].id")).longValue();
    }
}
