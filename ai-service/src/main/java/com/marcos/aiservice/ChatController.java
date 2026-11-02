package com.marcos.aiservice;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat/{userId}")
    public String chat(@RequestBody String question, @RequestParam(defaultValue = "default") String conversationId, @PathVariable UUID userId) {
        return chatService.ask(question, conversationId, userId);
    }

    @PostMapping(value = "/chat/{userId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody String question, @RequestParam(defaultValue = "default") String conversationId, @PathVariable UUID userId) {
        return chatService.stream(question, conversationId, userId);
    }

    @GetMapping("/chat/history")
    public List<Message> history(@RequestParam(defaultValue = "default") String conversationId) {
        return chatService.history(conversationId);
    }

    @GetMapping("/chat/history/short")
    public List<String> shortHistory(@RequestParam(defaultValue = "default") String conversationId) {
        return chatService.shortHistory(conversationId);
    }

    @DeleteMapping("/chat/history")
    public void clearHistory(@RequestParam(defaultValue = "default") String conversationId) {
        chatService.clearHistory(conversationId);
    }
}