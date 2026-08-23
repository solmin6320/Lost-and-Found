package com.example.lostandfound.jwt;

import com.example.lostandfound.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer "; // Bearer 토큰 방식(공백 한칸 포함)

    private final JwtTokenProvider jwtTokenProvider;



    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        // 토큰이 유효하고 리프레시 토큰이 아닐 때만 인증 처리
        if (token != null && jwtTokenProvider.validateToken(token) && !jwtTokenProvider.isRefreshToken(token)) {

            // DB 조회 없이 토큰의 sub(id)만으로 UserDetails 구성
            Long id = jwtTokenProvider.getMemberId(token);
            CustomUserDetails userDetails = new CustomUserDetails(id);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // 인증된 사용자 이기 때문에 비밀번호는 null
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 인증된 사용자 정보를 SecurityContextHolder에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 다음 필터에게 요청 위임
        filterChain.doFilter(request, response);
    }


    // Authorization 헤더에서 "Bearer "를 제거하고 순수 토큰만 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER); // 헤더 추출

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null; // 그 외는 모두 null
    }
}
