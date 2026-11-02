package com.marcos.aiservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    public String ask(String question) {
        return chatClient.prompt().user(question).call().content();
    }

    public Flux<String> stream(String question) {
        return chatClient.prompt().user(question).stream().content();
    }
}
