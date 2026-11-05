package com.marcos.aiservice.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public String chat(
            @RequestBody String question, @RequestParam(defaultValue = "default") String conversationId,
            @RequestParam(defaultValue = "") List<UUID> fileIds, Authentication auth
    ) {
        return chatService.ask(question, conversationId, userId(auth), fileIds);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestBody String question, @RequestParam(defaultValue = "default") String conversationId,
            @RequestParam(defaultValue = "") List<UUID> fileIds, Authentication auth
    ) {
        return chatService.stream(question, conversationId, userId(auth), fileIds);
    }

    @GetMapping("/conversations")
    public List<ConversationView> conversations(Authentication auth) {
        return chatService.conversations(userId(auth));
    }

    @GetMapping("/history")
    public List<MessageView> history(@RequestParam(defaultValue = "default") String conversationId, Authentication auth) {
        return chatService.history(conversationId, userId(auth));
    }

    @DeleteMapping("/history")
    public void clearHistory(@RequestParam(defaultValue = "default") String conversationId, Authentication auth) {
        chatService.clearHistory(conversationId, userId(auth));
    }

    private static UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}