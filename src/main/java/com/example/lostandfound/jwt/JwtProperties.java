package com.example.lostandfound.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated // 검증 애노테이션을 실제로 동작시킴
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(

        // JWT 서명용 비밀키
        // 환경변수 JWT_SECRET에서 주입
        @NotBlank
        String secret,

        // 액세스 토큰 유효시간(ms)
        @Positive // 0 또는 음수면 기동 실패
        long accessTokenExpiration, // long(원시타입)이라 yml 누락시 바인딩 실패 -> 기동 실패로 즉시 발견

        // 리프레시 토큰 유효시간(ms)
        @Positive
        long refreshTokenExpiration // 위와 동일한 이유로 long + @Positive
) {

}
