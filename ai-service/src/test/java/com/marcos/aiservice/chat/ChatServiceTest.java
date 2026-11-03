package com.marcos.aiservice.chat;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.SessionService;
import java.util.UUID;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final ChatService service = new ChatService(chatClient, mock(SessionService.class));
    private final UUID userId = UUID.randomUUID();

    @Test
    void askReturnsContent() {
        when(chatClient.prompt().user("hola").advisors(any(Consumer.class)).call().content()).thenReturn("buenas");
        assertThat(service.ask("hola", "1", userId)).isEqualTo("buenas");
    }

    @Test
    void streamReturnsChunks() {
        when(chatClient.prompt().user("hola").advisors(any(Consumer.class)).stream().content()).thenReturn(Flux.just("Hola", " mundo"));
        assertThat(service.stream("hola", "1", userId).collectList().block()).containsExactly("Hola", " mundo");
    }
}