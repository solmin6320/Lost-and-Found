package com.example.lostandfound.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

// CORS 허용 출처
@Validated
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(

        // 비어 있으면 기동 실패
        @NotEmpty
        List<@Pattern(regexp = "^https?://[^/*\\s]+$", message = "출처는 scheme://host[:port] 형태(끝 슬래시 · * 금지)여야 합니다") String>
        allowedOrigins
) {

}
