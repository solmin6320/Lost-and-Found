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
import java.util.List;
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


    @Test
    @DisplayName("목록 썸네일은 가장 먼저 올린 사진이고 사진 없는 글은 null")
    void listThumbnail() throws Exception {
        Long withImages = createPost(owner, image("a.jpg"), image("b.jpg"));
        Long noImage = createPost(owner);


        String firstKey = jdbcTemplate.queryForObject(
                "SELECT file_path FROM post_image WHERE post_id = ? ORDER BY image_id LIMIT 1",
                String.class, withImages);

        String body = mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        List<String> thumb = JsonPath.read(body, "$.content[?(@.id == " + withImages + ")].thumbnailUrl");
        List<String> none = JsonPath.read(body, "$.content[?(@.id == " + noImage + ")].thumbnailUrl");

        assertThat(thumb).containsExactly("https://integration-test.example/" + firstKey);
        assertThat(none).containsExactly((String) null);
    }

    @Test
    @DisplayName("내가 쓴 글은 내 것만 최신순으로 나오고 status로 거를 수 있음")
    void myPosts() throws Exception {
        Long first = createPost(owner);
        Long second = createPost(owner);
        createPost(other);

        mockMvc.perform(changeStatus(first, owner, "DONE")).andExpect(status().isOk());

        // 같은 초에 만들어져도 id 역순이 두 번째 정렬 기준
        mockMvc.perform(get("/api/members/me/posts").header(AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(second));

        mockMvc.perform(get("/api/members/me/posts").param("status", "DONE").header(AUTHORIZATION, owner))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(first));
    }


    @Test
    @DisplayName("수정 때 새 사진을 올리면 기존 사진이 모두 교체되고 옛 사진은 커밋 뒤 S3에서 지워짐")
    void updateReplacesImages() throws Exception {
        Long postId = createPost(owner, image("a.jpg"), image("b.jpg"));
        List<String> oldKeys = jdbcTemplate.queryForList(
                "SELECT file_path FROM post_image WHERE post_id = ?", String.class, postId);

        MockMultipartHttpServletRequestBuilder request = multipart(HttpMethod.PUT, "/api/posts/{id}", postId);
        request.file(image("c.jpg"));

        mockMvc.perform(postForm(request).header(AUTHORIZATION, owner))
                .andExpect(status().isOk());

        List<String> newKeys = jdbcTemplate.queryForList(
                "SELECT file_path FROM post_image WHERE post_id = ?", String.class, postId);

        assertThat(newKeys).hasSize(1).doesNotContainAnyElementsOf(oldKeys);
        verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("목록 상태 필터는 여러 값을 받아 그중 하나인 글만 주고, 값이 하나, 없음, 잘못된 값도 그대로 동작")
    void listStatusMulti() throws Exception {
        Long open = createPost(owner);
        Long inProgress = createPost(owner);
        Long done = createPost(owner);
        mockMvc.perform(changeStatus(inProgress, owner, "IN_PROGRESS")).andExpect(status().isOk());

        mockMvc.perform(changeStatus(done, owner, "DONE")).andExpect(status().isOk());

        // 프론트 기본값 "진행 중"
        String body = mockMvc.perform(get("/api/posts").param("status", "OPEN", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$.content[*].id");

        assertThat(ids).extracting(Number::longValue).containsExactlyInAnyOrder(open, inProgress);


        // 값 하나는 지금처럼
        mockMvc.perform(get("/api/posts").param("status", "DONE"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(done));

        // 안 보내면 전체
        mockMvc.perform(get("/api/posts"))
                .andExpect(jsonPath("$.page.totalElements").value(3));

        // 없는 상태 값은 400
        mockMvc.perform(get("/api/posts").param("status", "OPEN", "XX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
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