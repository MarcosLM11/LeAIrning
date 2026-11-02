package com.marcos.aiservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    @Test
    void chatReturnsModelAnswer() throws Exception {
        given(chatService.ask("hola")).willReturn("buenas");

        mockMvc.perform(post("/chat").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isOk())
                .andExpect(content().string("buenas"));
    }

    @Test
    void streamEmitsEachChunkAsSse() throws Exception {
        given(chatService.stream("hola")).willReturn(Flux.just("Hola", " mundo"));

        var started = mockMvc.perform(post("/chat/stream").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string("data: Hola\n\ndata:  mundo\n\n"));
    }
}