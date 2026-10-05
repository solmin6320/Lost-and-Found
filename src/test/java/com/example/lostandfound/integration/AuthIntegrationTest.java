package com.example.lostandfound.integration;

import com.example.lostandfound.support.IntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestSupport {

    private static final String EMAIL = "auth@test.com";
    private static final String NICKNAME = "인증테스트";

    @Test
    @DisplayName("가입 → 로그인 → 내 정보 → 재발급 → 로그아웃이 이어진다")
    void fullAuthFlow() throws Exception {
        signup(EMAIL, NICKNAME);

        MvcResult login = login(EMAIL, PASSWORD).andExpect(status().isOk()).andReturn();
        Cookie refresh = login.getResponse().getCookie(REFRESH_COOKIE);

        // 리프레시 토큰은 바디가 아니라 httpOnly 쿠키로만 인증 경로에만 실린다
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/auth");
        assertThat(redisTemplate.hasKey("refresh:" + memberIdOf(EMAIL))).isTrue();

        mockMvc.perform(get("/api/members/me").header(AUTHORIZATION, "Bearer " + accessTokenOf(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));

        MvcResult reissue = mockMvc.perform(post("/api/auth/reissue").cookie(refresh))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/api/auth/logout").header(AUTHORIZATION, "Bearer " + accessTokenOf(reissue)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));

        // 로그아웃하면 Redis에서 지워져 재발급이 불가함
        assertThat(redisTemplate.hasKey("refresh:" + memberIdOf(EMAIL))).isFalse();
        mockMvc.perform(post("/api/auth/reissue").cookie(reissue.getResponse().getCookie(REFRESH_COOKIE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_MISMATCH"));
    }

    @Test
    @DisplayName("재발급하면 이전 리프레시 토큰은 다시 쓸 수 없음")
    void oldRefreshTokenRejectedAfterReissue() throws Exception {
        signup(EMAIL, NICKNAME);
        Cookie first = login(EMAIL, PASSWORD).andReturn().getResponse().getCookie(REFRESH_COOKIE);

        mockMvc.perform(post("/api/auth/reissue").cookie(first))
                .andExpect(status().isOk());

        // 탈취된 옛 토큰의 재사용 차단(로테이션)
        mockMvc.perform(post("/api/auth/reissue").cookie(first))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_MISMATCH"));
    }

    @Test
    @DisplayName("비밀번호를 5번 틀리면 6번째는 맞아도 423")
    void lockedAfterFiveFailures() throws Exception {
        signup(EMAIL, NICKNAME);

        for (int i = 0; i < 5; i++) {
            login(EMAIL, "wrong-password")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        login(EMAIL, PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    @DisplayName("토큰 없이 로그아웃하면 401")
    void logoutWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("같은 이메일로 다시 가입하면 409")
    void duplicateEmail() throws Exception {
        signup(EMAIL, NICKNAME);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", EMAIL, "password", PASSWORD, "nickname", "다른닉네임"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }


    @Test
    @DisplayName("비밀번호를 바꾸면 기존 리프레시 토큰을 폐기하고 새 비밀번호로만 로그인됨")
    void passwordChangeRevokesSessions() throws Exception {
        signup(EMAIL, NICKNAME);
        MvcResult login = login(EMAIL, PASSWORD).andExpect(status().isOk()).andReturn();
        Cookie refresh = login.getResponse().getCookie(REFRESH_COOKIE);

        mockMvc.perform(patch("/api/members/me/password").header(AUTHORIZATION, "Bearer " + accessTokenOf(login))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "password", "newPassword456!"))))
                .andExpect(status().isNoContent());

        // 다른 기기에 있던 리프레시 토큰으로는 더 이상 재발급 불가
        mockMvc.perform(post("/api/auth/reissue").cookie(refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_MISMATCH"));

        login(EMAIL, PASSWORD).andExpect(status().isUnauthorized());
        login(EMAIL, "newPassword456!").andExpect(status().isOk());
    }

    @Test
    @DisplayName("이메일은 공백을 걷고 소문자로 저장, 대소문자가 달라도 같은 계정으로 로그인")
    void emailNormalized() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "  Mixed@Test.COM ",
                                "password", PASSWORD,
                                "nickname", "정규화"))))
                .andExpect(status().isCreated());

        String saved = jdbcTemplate.queryForObject("SELECT email FROM member", String.class);
        assertThat(saved).isEqualTo("mixed@test.com");

        login("MIXED@test.com", PASSWORD)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("대소문자만 바꿔 틀려도 같은 잠금으로 세어 6번째는 423")
    void lockNotBypassedByCase() throws Exception {
        signup(EMAIL, NICKNAME);

        String[] variants = {
                "AUTH@test.com",
                "Auth@Test.com",
                "auth@TEST.com",
                "aUtH@test.com",
                "auth@test.COM"
        };

        for (String variant : variants) {
            login(variant, "wrong-password")
                    .andExpect(status().isUnauthorized());
        }


        login("AuTh@TeSt.CoM", PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    @DisplayName("닉네임은 앞뒤 공백 제거, 자기 닉네임 대소문자 변경은 허용, 남의 것은 409")
    void nicknameNormalized() throws Exception {
        String me = bearerOf("me@test.com", "Kim");
        signup("other@test.com", "Lee");

        // 자기 닉네임의 대소문자만 바꾸기
        mockMvc.perform(changeNickname(me, "kim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("kim"));

        // 남의 닉네임을 대소문자, 공백만 바꿔 쓰기
        mockMvc.perform(changeNickname(me, "  LEE  "))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));

        // 앞뒤 공백은 없애고 저장
        mockMvc.perform(changeNickname(me, "  테스터  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("테스터"));
    }


    private RequestBuilder changeNickname(String bearer, String nickname) {
        return patch("/api/members/me")
                .header(AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nickname", nickname)));
    }
}