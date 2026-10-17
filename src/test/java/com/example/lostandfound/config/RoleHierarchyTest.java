package com.example.lostandfound.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 역할 위계 단위 테스트
class RoleHierarchyTest {

    private final RoleHierarchy roleHierarchy = SecurityConfig.roleHierarchy();


    @Test
    @DisplayName("관리자는 매니저, 회원 권한을 모두 가짐")
    void admin_reachesAll() {

        assertThat(reachable("ROLE_ADMIN"))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_USER");
    }

    @Test
    @DisplayName("매니저는 회원 권한을 갖지만 관리자 권한은 없음")
    void manager_reachesUserOnly() {

        assertThat(reachable("ROLE_MANAGER"))
                .containsExactlyInAnyOrder("ROLE_MANAGER", "ROLE_USER");
    }

    @Test
    @DisplayName("회원은 자기 권한만 가질 수 있음")
    void user_reachesItselfOnly() {
        assertThat(reachable("ROLE_USER"))
                .containsExactly("ROLE_USER");
    }





    // 역할 하나가 실제로 갖게 되는 권한 목록
    private List<String> reachable(String role) {

        return roleHierarchy.getReachableGrantedAuthorities(AuthorityUtils.createAuthorityList(role))
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
