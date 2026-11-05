package com.marcos.aiservice.chat;

import static java.util.stream.Collectors.groupingBy;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String TITLE = "title";
    private static final int TITLE_MAX = 40;

    private final ChatClient chatClient;
    private final SessionService sessionService;
    private final AnswerSourceRepository answerSources;

    public String ask(String question, String conversationId, UUID userId, List<UUID> fileIds) {
        return prompt(question, conversationId, userId, fileIds)
                .call()
                .content();
    }

    public Flux<String> stream(String question, String conversationId, UUID userId, List<UUID> fileIds) {
        return prompt(question, conversationId, userId, fileIds)
                .stream()
                .content();
    }

    private ChatClient.ChatClientRequestSpec prompt(String question, String conversationId, UUID userId, List<UUID> fileIds) {
        ensureSession(sessionId(userId, conversationId), userId, question);
        return chatClient.prompt()
                .user(question)
                .advisors(a -> a
                        .param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId(userId, conversationId))
                        .param(SessionMemoryAdvisor.USER_ID_CONTEXT_KEY, userId.toString())
                        .param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filter(userId, fileIds))
                );
    }

    // Always the user's own documents; optionally narrowed to the given files (empty = all of them).
    private static Filter.Expression filter(UUID userId, List<UUID> fileIds) {
        var b = new FilterExpressionBuilder();
        var own = b.eq("userId", userId.toString());
        return fileIds.isEmpty() ? own.build() : b.and(own, b.in("fileId", fileIds.stream().map(UUID::toString).toArray())).build();
    }

    private static String sessionId(UUID userId, String conversationId) {
        return userId + ":" + conversationId;
    }

    private void ensureSession(String sessionId, UUID userId, String question) {
        if (sessionService.findById(sessionId) == null) {
            sessionService.create(CreateSessionRequest.builder()
                    .id(sessionId)
                    .userId(userId.toString())
                    .metadata(TITLE, title(question))
                    .build());
        }
    }

    static String title(String question) {
        var oneLine = question.strip().replaceAll("\\s+", " ");
        return oneLine.length() > TITLE_MAX ? oneLine.substring(0, TITLE_MAX).strip() + "…" : oneLine;
    }

    public List<ConversationView> conversations(UUID userId) {
        var prefix = userId + ":";
        return sessionService.findByUserId(userId.toString()).stream()
                .sorted(Comparator.comparing(Session::createdAt).reversed())
                .map(s -> new ConversationView(s.id().substring(prefix.length()), (String) s.metadata().get(TITLE), s.createdAt()))
                .toList();
    }

    public List<MessageView> history(String conversationId, UUID userId) {
        var events = sessionService.getEvents(sessionId(userId, conversationId), EventFilter.active()).stream()
                .filter(e -> !e.getMetadata().containsKey(SessionEvent.METADATA_SYNTHETIC))
                .toList();

        var sources = answerSources.findByEventIdInOrderByFilenameAscPageAsc(events.stream().map(SessionEvent::getId).toList()).stream()
                .collect(groupingBy(AnswerSource::getEventId));

        return events.stream()
                .map(e -> new MessageView(e.getMessage().getMessageType().name().toLowerCase(), e.getMessage().getText(), e.getTimestamp(), sources.getOrDefault(e.getId(), List.of()).stream().map(MessageView.Source::of).toList()))
                .toList();
    }

    public void clearHistory(String conversationId, UUID userId) {
        sessionService.delete(sessionId(userId, conversationId));
        answerSources.deleteBySessionId(sessionId(userId, conversationId));
    }
}