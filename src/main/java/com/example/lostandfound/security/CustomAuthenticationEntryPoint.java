package com.example.lostandfound.security;

import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
// 인증되지 않은 요청을 401 + 공통 에러 형식으로 응답
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Spring Boot가 등록해 둔 빈을 재사용
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorCode errorCode = ErrorCode.INVALID_ACCESS_TOKEN;

        response.setStatus(errorCode.getStatus().value()); // 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name()); // 한글 메시지 보존

        // 필터 체인 직접 직렬화
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(errorCode));
    }
}
