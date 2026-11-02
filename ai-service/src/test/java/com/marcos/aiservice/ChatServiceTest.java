package com.marcos.aiservice;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final ChatService service = new ChatService(chatClient);

    @Test
    void askReturnsContent() {
        when(chatClient.prompt().user("hola").call().content()).thenReturn("buenas");
        assertThat(service.ask("hola")).isEqualTo("buenas");
    }

    @Test
    void streamReturnsChunks() {
        when(chatClient.prompt().user("hola").stream().content()).thenReturn(Flux.just("Hola", " mundo"));
        assertThat(service.stream("hola").collectList().block()).containsExactly("Hola", " mundo");
    }
}