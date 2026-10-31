package com.example.lostandfound.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// public-data 설정 바인딩
@ConfigurationProperties(prefix = "public-data")
public record PublicDataProperties(

        boolean enabled, // false면 수집하지 않음
        String serviceKey, // 디코딩 키
        String baseUrl
) {
}
