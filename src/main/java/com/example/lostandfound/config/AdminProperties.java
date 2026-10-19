package com.example.lostandfound.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;

// 관리자 이메일 목록
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(

        List<String> emails
) {

    // 비어 있으면 빈 목록, 공백 제거 및 소문자
    public AdminProperties {
        emails = (emails == null) ? List.of() : emails.stream()
                                                .filter(email -> email != null && !email.isBlank())
                                                .map(email -> email.strip().toLowerCase(Locale.ROOT))
                                                .distinct()
                                                .toList();
    }
}
