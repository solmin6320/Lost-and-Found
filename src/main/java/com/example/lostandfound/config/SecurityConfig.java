package com.example.lostandfound.config;

import com.example.lostandfound.jwt.JwtAuthenticationFilter;
import com.example.lostandfound.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    // BCrypt 해시(회원 가입 시 encode, 로그인 시 matches 사용)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // 인증 매니저를 빈으로 노출
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 액세스 토큰을 쿠키로 전달하지 않으므로 CSRF 방어를 꺼둠
                .csrf(csrf -> csrf.disable())

                // 리프레시 토큰을 위해 CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 서버가 세션을 사용하지 않음(무상태성 유지)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 기본 폼 로그인 및 HTTP 인증 미사용
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                        // 인증 실패를 401 + 공통 형식으로 응답
                        .exceptionHandling(ex ->
                                ex.authenticationEntryPoint(customAuthenticationEntryPoint))

                // 경로별 접근 권한
                .authorizeHttpRequests(auth -> auth
                        //회원가입, 로그인, 토큰 재발급은 인증 없이 접근 가능
                        .requestMatchers("/api/auth/signup", "/api/auth/reissue", "/api/auth/login").permitAll()

                        // 게시글 조회는 비로그인도 열람 가능
                        .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터를 폼로그인 필터 자리 보다 앞에 배치
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
    }

    // CORS
    // 프론트엔드가 다른 도메인에서 쿠키를 포함해 요청하므로 필요함
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // 배포 시 실제 도메인 추가(지금은 개발용)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // 쿠키 전송 허용(리프레시 토큰)
        configuration.setMaxAge(3600L); // preflight 응답 캐시(초)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
