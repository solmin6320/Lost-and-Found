package com.example.lostandfound.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThat;

// 자격 증명 방식 선택 단위 테스트
class S3ConfigTest {

    @Test
    @DisplayName("키가 있으면 키 방식")
    void staticWhenKeyGiven() {
        S3Config config = configWith("local-access", "local-secret");

        AwsCredentialsProvider provider = config.credentialsProvider();

        assertThat(provider).isInstanceOf(StaticCredentialsProvider.class);
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("local-access");
    }

    @Test
    @DisplayName("키가 비어 있으면 기본 체인")
    void defaultChainWhenKeyBlank() {
        S3Config config = configWith("", "");

        assertThat(config.credentialsProvider()).isInstanceOf(DefaultCredentialsProvider.class);
    }

    private S3Config configWith(String accessKey, String secretKey) {
        AwsProperties properties = new AwsProperties(
                "ap-northeast-2",
                new AwsProperties.S3("test-bucket", "https://example.com"),
                new AwsProperties.Credentials(accessKey, secretKey)
        );

        return new S3Config(properties);
    }
}