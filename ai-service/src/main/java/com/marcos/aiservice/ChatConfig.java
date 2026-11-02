package com.marcos.aiservice;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService) {
        return SessionMemoryAdvisor.builder(sessionService)
                .compactionTrigger(request -> request.currentEventCount() > 100)
                .compactionStrategy(SlidingWindowCompactionStrategy.builder().maxEvents(90).build())
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, SessionMemoryAdvisor sessionMemoryAdvisor) {

        return builder
                .defaultSystem("Eres un asistente útil y conciso.")
                .defaultAdvisors(
                        sessionMemoryAdvisor,
                        SimpleLoggerAdvisor.builder().build()
                )
                .build();
    }
}