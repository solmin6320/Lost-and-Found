package com.example.lostandfound.integration;

import com.example.lostandfound.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentIntegrationTest extends IntegrationTestSupport {

    private String writer;      // 글쓴이
    private String commenter;   // 댓글을 단 사람
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        writer = bearerOf("writer@test.com", "글쓴이");
        commenter = bearerOf("commenter@test.com", "댓글러");
        postId = writePost(writer);
    }

    @Test
    @DisplayName("댓글은 쓴 사람만 고치고 지움 | 글쓴이도 남의 댓글은 수정 불가")
    void onlyAuthorEditsComment() throws Exception {
        Long commentId = writeComment(commenter, "봤어요");

        mockMvc.perform(editComment(commentId, writer, "고침"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCESS"));

        mockMvc.perform(delete("/api/comments/{id}", commentId).header(AUTHORIZATION, writer))
                .andExpect(status().isForbidden());

        mockMvc.perform(editComment(commentId, commenter, "2층에서 봤어요"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("2층에서 봤어요"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(delete("/api/comments/{id}", commentId).header(AUTHORIZATION, commenter))
                .andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM comment", Long.class)).isZero();
    }

    @Test
    @DisplayName("댓글 21개면 상세는 20개 미리보기 + 목록 2쪽에 마지막 1개")
    void previewAndPaging() throws Exception {
        for (int i = 1; i <= 21; i++) {
            writeComment(commenter, "댓글 " + i);
        }

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(jsonPath("$.comments.length()").value(20))
                .andExpect(jsonPath("$.totalCommentCount").value(21));


        mockMvc.perform(get("/api/posts/{id}/comments", postId).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("댓글 21"));
    }

    @Test
    @DisplayName("없는 글에는 댓글을 쓰거나 볼 수 없음")
    void missingPost() throws Exception {
        mockMvc.perform(post("/api/posts/{id}/comments", 999_999L).header(AUTHORIZATION, commenter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "봤어요"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));

        mockMvc.perform(get("/api/posts/{id}/comments", 999_999L))
                .andExpect(status().isNotFound());
    }


    private Long writeComment(String token, String content) throws Exception {
        String body = mockMvc.perform(post("/api/posts/{id}/comments", postId).header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", content))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();


        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }


    private RequestBuilder editComment(Long commentId, String token, String content) {
        return put("/api/comments/{id}", commentId).header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("content", content)));
    }

}