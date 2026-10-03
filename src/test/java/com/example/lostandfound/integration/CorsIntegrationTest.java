package com.example.lostandfound.integration;

import com.example.lostandfound.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsIntegrationTest extends IntegrationTestSupport {

    private static final String ALLOWED = "http://localhost:5173";  // application.yml 기본 목록
    private static final String ALB_HOST = "internal-alb.example";   // CloudFront가 바꿔 보내는 Host

    @Test
    @DisplayName("허용한 출처의 프리플라이트는 200과 허용 헤더를 받음")
    void preflightAllowed() throws Exception {
        mockMvc.perform(options("/api/posts")
                        .header(HttpHeaders.ORIGIN, ALLOWED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))

                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("모르는 출처의 프리플라이트는 403")
    void preflightRejected() throws Exception {
        mockMvc.perform(options("/api/posts")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Host가 ALB로 바뀌어 들어와도 허용한 출처면 로그인 성공")
    void allowedOriginBehindProxy() throws Exception {
        signup("cors@test.com", "코스테스트");

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.HOST, ALB_HOST)
                        .header(HttpHeaders.ORIGIN, ALLOWED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "cors@test.com", "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Host가 ALB로 바뀌고 출처가 목록에 없으면 403 - 운영 출처를 빠뜨렸을 때의 증상")
    void missingOriginBehindProxy() throws Exception {
        signup("cors@test.com", "코스테스트");

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.HOST, ALB_HOST)
                        .header(HttpHeaders.ORIGIN, "https://dxxxx.cloudfront.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "cors@test.com", "password", PASSWORD))))
                .andExpect(status().isForbidden());
    }
}