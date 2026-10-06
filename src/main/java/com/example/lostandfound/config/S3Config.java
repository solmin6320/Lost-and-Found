package com.example.lostandfound.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// S3Client는 직접 등록(스프링 빈이 아님)
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final AwsProperties awsProperties;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    // 키가 있으면 키, 없으면 기본 체인
    AwsCredentialsProvider credentialsProvider() {
        AwsProperties.Credentials credentials = awsProperties.credentials();

        if (credentials == null || !StringUtils.hasText(credentials.accessKey())) {
            return DefaultCredentialsProvider.create();
        }

        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(credentials.accessKey(), credentials.secretKey())
        );
    }
}
