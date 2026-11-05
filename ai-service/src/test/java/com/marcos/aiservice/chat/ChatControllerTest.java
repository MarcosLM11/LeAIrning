package com.marcos.aiservice.chat;

import com.marcos.aiservice.security.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void chatReturnsModelAnswer() throws Exception {
        given(chatService.ask("hola", "1", userId, List.of())).willReturn("buenas");

        mockMvc.perform(post("/api/v1/chat").with(jwt().jwt(j -> j.subject(userId.toString()))).param("conversationId", "1").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isOk())
                .andExpect(content().string("buenas"));
    }

    @Test
    void chatPassesTheSelectedFileIds() throws Exception {
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        given(chatService.ask("hola", "1", userId, List.of(a, b))).willReturn("buenas");

        mockMvc.perform(post("/api/v1/chat").with(jwt().jwt(j -> j.subject(userId.toString()))).param("conversationId", "1")
                        .param("fileIds", a + "," + b).contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isOk())
                .andExpect(content().string("buenas"));
    }

    @Test
    void streamEmitsEachChunkAsSse() throws Exception {
        given(chatService.stream("hola", "1", userId, List.of())).willReturn(Flux.just("Hola", " mundo"));

        var started = mockMvc.perform(post("/api/v1/chat/stream").with(jwt().jwt(j -> j.subject(userId.toString()))).param("conversationId", "1").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string("data: Hola\n\ndata:  mundo\n\n"));
    }

    @Test
    void conversationsReturnsTheUsersChats() throws Exception {
        given(chatService.conversations(userId)).willReturn(List.of(new ConversationView("a", "Kafka vs RabbitMQ", Instant.parse("2026-11-02T10:00:00Z"))));

        mockMvc.perform(get("/api/v1/chat/conversations").with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("a"))
                .andExpect(jsonPath("$[0].title").value("Kafka vs RabbitMQ"));
    }

    @Test
    void historyReturnsRoleAndText() throws Exception {
        given(chatService.history("a", userId)).willReturn(List.of(new MessageView("user", "hola", Instant.parse("2026-11-02T10:00:00Z"))));

        mockMvc.perform(get("/api/v1/chat/history").param("conversationId", "a").with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].text").value("hola"));
    }

    @Test
    void chatWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/chat").param("conversationId", "1").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnauthorized());
    }
}
