package com.marcos.aiservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"spring.ai.openai.api-key=test-key", "spring.kafka.listener.auto-startup=false"})
class AiServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
