package com.example.lostandfound;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@Tag("integration")
class LostAndFoundApplicationTests {

    @Test
    void contextLoads() {
    }

}
