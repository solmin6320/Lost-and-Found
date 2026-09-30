package com.example.lostandfound.integration;

import com.example.lostandfound.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostIntegrationTest extends IntegrationTestSupport {

    private String owner;   // 작성자 토큰
    private String other;   // 다른 회원 토큰

    @BeforeEach
    void setUp() throws Exception {
        owner = bearerOf("owner@test.com", "작성자");
        other = bearerOf("other@test.com", "다른사람");
    }

    @Test
    @DisplayName("사진 2장으로 등록하면 상세에 주소가 붙고, 조회수는 회원당 한 번만 오름")
    void createAndDetail() throws Exception {
        Long postId = createPost(owner, image("a.jpg"), image("b.png"));

        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertThat(count("post_image")).isEqualTo(2);


        mockMvc.perform(get("/api/posts/{id}", postId).header(AUTHORIZATION, other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].url").value(startsWith("https://integration-test.example/posts/" + postId + "/")))
                .andExpect(jsonPath("$.viewCount").value(1));

        mockMvc.perform(get("/api/posts/{id}", postId).header(AUTHORIZATION, other))
                .andExpect(jsonPath("$.viewCount").value(1));

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1));
    }

    @Test
    @DisplayName("다른 사람은 수정, 삭제, 상태 변경 모두 403이고 글은 그대로임")
    void othersForbidden() throws Exception {
        Long postId = createPost(owner);

        mockMvc.perform(postForm(multipart(HttpMethod.PUT, "/api/posts/{id}", postId)).header(AUTHORIZATION, other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ACCESS"));

        mockMvc.perform(delete("/api/posts/{id}", postId).header(AUTHORIZATION, other))
                .andExpect(status().isForbidden());

        mockMvc.perform(changeStatus(postId, other, "DONE"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.title").value("검은 지갑"));
    }

    @Test
    @DisplayName("상태는 OPEN -> IN_PROGRESS -> DONE, DONE에서는 되돌릴 수 없음")
    void statusTransition() throws Exception {
        Long postId = createPost(owner);

        mockMvc.perform(changeStatus(postId, owner, "IN_PROGRESS")).andExpect(status().isOk());
        mockMvc.perform(changeStatus(postId, owner, "DONE")).andExpect(status().isOk());
        mockMvc.perform(changeStatus(postId, owner, "OPEN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("삭제하면 댓글, 사진 행이 함께 지워지고, S3 삭제는 커밋 뒤에 나감")
    void deleteCascades() throws Exception {

        Long postId = createPost(owner, image("a.jpg"));
        mockMvc.perform(post("/api/posts/{id}/comments", postId).header(AUTHORIZATION, other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "봤어요"))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/posts/{id}", postId).header(AUTHORIZATION, owner))
                .andExpect(status().isNoContent());

        assertThat(count("post")).isZero();
        assertThat(count("post_image")).isZero();
        assertThat(count("comment")).isZero();

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("비로그인은 글을 등록할 수 없음")
    void createWithoutLogin() throws Exception {
        mockMvc.perform(postForm(multipart("/api/posts")))
                .andExpect(status().isUnauthorized());
        assertThat(count("post")).isZero();
    }



    private Long createPost(String token, MockMultipartFile... images) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/posts");
        for (MockMultipartFile image : images) {
            request.file(image);
        }

        String body = mockMvc.perform(postForm(request).header(AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    // 등록·수정 공통 폼 필드
    private MockMultipartHttpServletRequestBuilder postForm(MockMultipartHttpServletRequestBuilder request) {
        return (MockMultipartHttpServletRequestBuilder) request
                .param("type", "LOST")
                .param("title", "검은 지갑")
                .param("content", "신흥역 4번 출구 근처에서 잃어버렸습니다")
                .param("category", "WALLET")
                .param("location", "신흥역")
                .param("lostFoundDate", LocalDate.now().minusDays(1).toString());
    }

    private org.springframework.test.web.servlet.RequestBuilder changeStatus(Long postId, String token, String status) {
        return patch("/api/posts/{id}/status", postId).header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", status)));
    }

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile("images", filename, "image/jpeg", new byte[] {1, 2, 3});
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}