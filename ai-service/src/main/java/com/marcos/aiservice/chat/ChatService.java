package com.marcos.aiservice.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final SessionService sessionService;

    public String ask(String question, String conversationId, UUID userId) {
        return prompt(question, conversationId, userId)
                .call()
                .content();
    }

    public Flux<String> stream(String question, String conversationId, UUID userId) {
        return prompt(question, conversationId, userId)
                .stream()
                .content();
    }

    private ChatClient.ChatClientRequestSpec prompt(String question, String conversationId, UUID userId) {
        return chatClient.prompt()
                .user(question)
                .advisors(a -> a
                        .param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId(userId, conversationId))
                        .param(SessionMemoryAdvisor.USER_ID_CONTEXT_KEY, userId.toString())
                );
    }

    private static String sessionId(UUID userId, String conversationId) {
        return userId + ":" + conversationId;
    }

    public List<Message> history(String conversationId, UUID userId) {
        return sessionService.getMessages(sessionId(userId, conversationId));
    }

    public List<String> shortHistory(String conversationId, UUID userId) {
        return sessionService.getMessages(sessionId(userId, conversationId)).stream()
                .map(m -> m.getMessageType() + ": " + m.getText())
                .toList();
    }

    public void clearHistory(String conversationId, UUID userId) {
        sessionService.delete(sessionId(userId, conversationId));
    }
}