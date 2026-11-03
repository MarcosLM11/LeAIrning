package com.marcos.aiservice.chat;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.SessionService;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final SessionService sessions = mock(SessionService.class);
    private final ChatService service = new ChatService(chatClient, sessions);
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

    @Test
    void historyIsScopedByUser() {
        var other = UUID.randomUUID();
        when(sessions.getMessages(userId + ":1")).thenReturn(List.of(new UserMessage("mío")));

        assertThat(service.shortHistory("1", userId)).containsExactly("USER: mío");
        assertThat(service.history("1", other)).isEmpty();

        service.clearHistory("1", other);
        verify(sessions).delete(other + ":1");
        verify(sessions, never()).delete(userId + ":1");
    }
}
