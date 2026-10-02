package com.example.lostandfound.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// 리프레시 토큰 쿠키 설정
@ConfigurationProperties(prefix = "auth.cookie")
public record AuthCookieProperties(

        // HTTPS에서만 쿠키 전송
        @DefaultValue("true")
        boolean secure

) {

}
