package com.example.lostandfound.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

// cloud.aws. * 설정 바인딩
@ConfigurationProperties(prefix = "cloud.aws")
public record AwsProperties(

        @NotBlank String region,
        S3 s3,
        Credentials credentials
) {

    public record S3(
            @NotBlank String bucket,
            // 객체 키 앞에 붙일 주소
            @NotBlank String baseUrl // 로컬 S3 / 운영 CloudFront

    ) {

    }

    public record Credentials(
            @NotBlank String accessKey,
            @NotBlank String secretKey
    ) {

    }
}
