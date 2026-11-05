package com.marcos.aiservice.chat;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final SessionService sessions = mock(SessionService.class);
    private final AnswerSourceRepository answerSources = mock(AnswerSourceRepository.class);
    private final ChatService service = new ChatService(chatClient, sessions, answerSources);
    private final UUID userId = UUID.randomUUID();

    @Test
    void askReturnsContent() {
        when(chatClient.prompt().user("hola").advisors(any(Consumer.class)).call().content()).thenReturn("buenas");
        assertThat(service.ask("hola", "1", userId, List.of())).isEqualTo("buenas");
    }

    @Test
    void firstQuestionCreatesTheSessionWithATrimmedTitle() {
        when(chatClient.prompt().user(any(String.class)).advisors(any(Consumer.class)).call().content()).thenReturn("ok");
        var question = "¿Cuáles son las diferencias principales entre Kafka y RabbitMQ en mensajería?";

        service.ask(question, "1", userId, List.of());

        var request = ArgumentCaptor.forClass(CreateSessionRequest.class);
        verify(sessions).create(request.capture());
        assertThat(request.getValue().id()).isEqualTo(userId + ":1");
        assertThat(request.getValue().userId()).isEqualTo(userId.toString());
        assertThat((String) request.getValue().metadata().get("title")).hasSizeLessThanOrEqualTo(41).endsWith("…").startsWith("¿Cuáles son las");
    }

    @Test
    void existingSessionIsNotCreatedAgain() {
        when(chatClient.prompt().user("hola").advisors(any(Consumer.class)).call().content()).thenReturn("ok");
        when(sessions.findById(userId + ":1")).thenReturn(Session.builder().id(userId + ":1").userId(userId.toString()).build());

        service.ask("hola", "1", userId, List.of());

        verify(sessions, never()).create(any());
    }

    @Test
    void historyAttachesSourcesToTheAnswerTheyBelongTo() {
        var question = SessionEvent.builder().sessionId(userId + ":1").message(new UserMessage("¿qué es Kafka?")).build();
        var answer = SessionEvent.builder().sessionId(userId + ":1").message(new AssistantMessage("Un log distribuido")).build();
        var fileId = UUID.randomUUID();
        when(sessions.getEvents(eq(userId + ":1"), any(EventFilter.class))).thenReturn(List.of(question, answer));
        when(answerSources.findByEventIdInOrderByFilenameAscPageAsc(List.of(question.getId(), answer.getId())))
                .thenReturn(List.of(new AnswerSource(answer.getId(), userId + ":1", fileId, "kafka.pdf", 12)));

        var history = service.history("1", userId);

        assertThat(history.get(0).sources()).isEmpty();
        assertThat(history.get(1).sources()).containsExactly(new MessageView.Source("kafka.pdf", 12));
    }

    @Test
    void conversationsListOnlyTheUsersChatsNewestFirstWithoutThePrefix() {
        var old = Session.builder().id(userId + ":a").userId(userId.toString()).createdAt(Instant.parse("2026-11-01T10:00:00Z")).metadata(Map.of("title", "viejo")).build();
        var recent = Session.builder().id(userId + ":b").userId(userId.toString()).createdAt(Instant.parse("2026-11-02T10:00:00Z")).metadata(Map.of("title", "nuevo")).build();
        when(sessions.findByUserId(userId.toString())).thenReturn(List.of(old, recent));

        assertThat(service.conversations(userId)).extracting(ConversationView::id, ConversationView::title)
                .containsExactly(tuple("b", "nuevo"), tuple("a", "viejo"));
    }

    @Test
    void streamReturnsChunks() {
        when(chatClient.prompt().user("hola").advisors(any(Consumer.class)).stream().content()).thenReturn(Flux.just("Hola", " mundo"));
        assertThat(service.stream("hola", "1", userId, List.of()).collectList().block()).containsExactly("Hola", " mundo");
    }

    @Test
    void historyIsScopedByUser() {
        var other = UUID.randomUUID();
        when(sessions.getEvents(eq(userId + ":1"), any(EventFilter.class))).thenReturn(List.of(
                SessionEvent.builder().sessionId(userId + ":1").message(new UserMessage("mío")).build(),
                SessionEvent.builder().sessionId(userId + ":1").message(new AssistantMessage("tuyo")).build()));

        assertThat(service.history("1", userId)).extracting(MessageView::role, MessageView::text)
                .containsExactly(tuple("user", "mío"), tuple("assistant", "tuyo"));
        assertThat(service.history("1", other)).isEmpty();

        service.clearHistory("1", other);
        verify(sessions).delete(other + ":1");
        verify(answerSources).deleteBySessionId(other + ":1");
        verify(answerSources, never()).deleteBySessionId(userId + ":1");
        verify(sessions, never()).delete(userId + ":1");
    }
}
