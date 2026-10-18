package com.example.lostandfound.jwt;

import com.example.lostandfound.entity.Role;
import com.example.lostandfound.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;


import static org.assertj.core.api.Assertions.assertThat;

// 토큰의 역할 클레임 단위 테스트
class JwtTokenProviderTest {

    // 테스트 전용 32바이트 키
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(new JwtProperties(SECRET, 300000L, 604800000L));


    @Test
    @DisplayName("액세스 토큰에는 회원 ID와 역할이 담김")
    void accessToken_containsRole() {

        String token = jwtTokenProvider.createAccessToken(authenticationOf(7L, Role.MANAGER));

        assertThat(jwtTokenProvider.getRole(token)).isEqualTo(Role.MANAGER);
        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰에는 역할을 담지 않아 USER로 읽음")
    void refreshToken_hasNoRole() {
        String refreshToken = jwtTokenProvider.createRefreshToken(authenticationOf(7L, Role.ADMIN));

        assertThat(jwtTokenProvider.getRole(refreshToken)).isEqualTo(Role.USER);
        assertThat(jwtTokenProvider.getRole(refreshToken)).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("역할은 ROLE_ 접두사가 붙은 권한 하나로 변환됨")
    void authorities_hasRolePrefix() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, Role.ADMIN);

        assertThat(userDetails.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }







    private Authentication authenticationOf(Long id, Role role) {

        CustomUserDetails userDetails = new CustomUserDetails(id, role);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
