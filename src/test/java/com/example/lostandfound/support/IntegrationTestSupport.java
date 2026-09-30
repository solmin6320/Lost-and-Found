package com.example.lostandfound.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// 통합 테스트 공통 틀
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "integration"})
@Tag("integration")
public abstract class IntegrationTestSupport {

    private static final String TEST_SCHEMA = "lostfound_test";
    private static final int TEST_REDIS_DB = 1;
    private static final List<String> REDIS_PREFIXES = List.of("refresh:", "login:fail", "view:");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    private LettuceConnectionFactory redisConnectionFactory;

    // 실제 S3로 요청이 가지 않도록 가짜로 바꿈
    @MockitoBean protected S3Client s3Client;

    @BeforeEach
    void checkBefore() {
        verifyTestStorage();
    }

    @AfterEach
    void cleanUp() {
        verifyTestStorage();

        // FK 때문에 자식 테이블부터
        jdbcTemplate.update("DELETE FROM comment");
        jdbcTemplate.update("DELETE FROM post_image");
        jdbcTemplate.update("DELETE FROM post");
        jdbcTemplate.update("DELETE FROM member");

        for (String prefix : REDIS_PREFIXES) {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    // 개발 DB나 Redis 0번에 붙으면 아무것도 지우지 않고 실패
    private void verifyTestStorage() {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);

        assertThat(schema).as("통합 테스트는 %s 에서만 돈다", TEST_SCHEMA).isEqualTo(TEST_SCHEMA);
        assertThat(redisConnectionFactory.getDatabase())
                .as("통합 테스트는 Redis %d번에서만 돈다", TEST_REDIS_DB).isEqualTo(TEST_REDIS_DB);
    }

}
