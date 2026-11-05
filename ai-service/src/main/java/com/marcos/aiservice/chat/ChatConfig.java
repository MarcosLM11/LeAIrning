package com.marcos.aiservice.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService, SourceRecorder sourceRecorder) {
        return SessionMemoryAdvisor.builder(sessionService)
                .responseEventIdGenerator(sourceRecorder)
                .compactionTrigger(request -> request.currentEventCount() > 100)
                .compactionStrategy(SlidingWindowCompactionStrategy.builder().maxEvents(90).build())
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, SessionMemoryAdvisor sessionMemoryAdvisor, VectorStore vectorStore) {

        return builder
                .defaultSystem("Eres un asistente útil y conciso.")
                .defaultAdvisors(
                        sessionMemoryAdvisor,
                        RetrievalAugmentationAdvisor.builder()
                                .documentRetriever(VectorStoreDocumentRetriever.builder()
                                        .vectorStore(vectorStore)
                                        .similarityThreshold(0.3)
                                        .topK(4)
                                        .build())
                                .queryAugmenter(ContextualQueryAugmenter.builder()
                                        .allowEmptyContext(true)
                                        .build())
                                .build(),
                        SimpleLoggerAdvisor.builder().build()
                )
                .build();
    }
}