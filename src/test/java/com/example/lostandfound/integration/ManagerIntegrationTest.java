package com.example.lostandfound.integration;

import com.example.lostandfound.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ManagerIntegrationTest extends IntegrationTestSupport {

    private String admin; // 관리자
    private Long targetId; // 임명, 해임 대상

    @BeforeEach
    void setUp() throws Exception {

        admin = bearerWithRole("admin@test.com", "관리자", "ADMIN");

        signup("target@test.com", "대상");
        targetId = memberIdOf("target@test.com");
    }

    @Test
    @DisplayName("관리자가 임명 -> 해임 | 역할이 바뀌고 대상의 리프레시 토큰이 지워짐")
    void appointAndDismiss()  throws Exception {
            login("target@test.com", PASSWORD).andExpect(status().isOk()); // 대상의 리프레시 토큰 저장

        assertThat(redisTemplate.hasKey("refresh:" + targetId)).isTrue();

        mockMvc.perform(post("/api/admin/managers/{id}", targetId).header(AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        assertThat(roleOf(targetId)).isEqualTo("MANAGER");
        assertThat(redisTemplate.hasKey("refresh:" + targetId)).isFalse(); // 다시 로그인

        mockMvc.perform(delete("/api/admin/managers/{id}", targetId).header(AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        assertThat(roleOf(targetId)).isEqualTo("USER");
    }

    @Test
    @DisplayName("다시 로그인하면 새 역할이 토큰에 담겨 매니저 권한을 씀")
    void reloginCarriesNewRole() throws Exception {

        mockMvc.perform(post("/api/admin/managers/{id}", targetId).header(AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        String manager = bearerOfLogin("target@test.com");

        mockMvc.perform(post("/api/admin/managers/{id}", memberIdOf("admin@test.com")).header(AUTHORIZATION, manager))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("일반 회원은 403, 비로그인은 401")
    void nonAdminDenied() throws Exception {

        String user = bearerOf("user@test.com", "회원");

        mockMvc.perform(post("/api/admin/managers/{id}", targetId).header(AUTHORIZATION, user))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/managers/{id}", targetId).header(AUTHORIZATION, user))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/managers/{id}", targetId))
                .andExpect(status().isUnauthorized());

        assertThat(roleOf(targetId)).isEqualTo("USER");
    }

    @Test
    @DisplayName("이미 매니저 임명, 매니저 아닌 회원 해임, 관리자 대상은 409, 없는 회원은 404")
    void invalidChanges() throws Exception {
        mockMvc.perform(delete("/api/admin/managers/{id}", targetId).header(AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_MANAGER"));

        mockMvc.perform(post("/api/admin/managers/{id}", targetId).header(AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/admin/managers/{id}", targetId).header(AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_MANAGER"));

        mockMvc.perform(post("/api/admin/managers/{id}", memberIdOf("admin@test.com")).header(AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADMIN_ROLE_LOCKED"));

        mockMvc.perform(post("/api/admin/managers/{id}", 99999L).header(AUTHORIZATION, admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));

    }

    // 가입 -> DB에서 역할 지정 -> 로그인
    private String bearerWithRole(String email, String nickname, String role) throws Exception {
        signup(email, nickname);

        jdbcTemplate.update("UPDATE member SET role = ? WHERE email = ?", role, email);

        return bearerOfLogin(email);
    }

    private String bearerOfLogin(String email) throws Exception {

        MvcResult result = login(email, PASSWORD).andExpect(status().isOk()).andReturn();

        return "Bearer " + accessTokenOf(result);
    }

    private String roleOf(Long memberId) {

        return jdbcTemplate.queryForObject("SELECT role FROM member WHERE member_id = ?", String.class, memberId);
    }
}
