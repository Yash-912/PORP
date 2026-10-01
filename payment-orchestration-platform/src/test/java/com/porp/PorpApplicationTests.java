package com.porp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PorpApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}