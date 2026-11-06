package com.marcos.aiservice.quizz;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.marcos.aiservice.security.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(QuizzController.class)
@Import(SecurityConfig.class)
class QuizzRetryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QuizzService quizzService;

    private final UUID userId = UUID.randomUUID();
    private final UUID quizzId = UUID.randomUUID();

    @Test
    void acceptsAndPassesUserEmailAndId() throws Exception {
        mockMvc.perform(post("/api/v1/quizzes/" + quizzId + "/retry")
                        .with(jwt().jwt(j -> j.subject(userId.toString()).claim("email", "a@a.com"))))
                .andExpect(status().isAccepted());

        verify(quizzService).retry(userId, "a@a.com", quizzId);
    }

    @Test
    void serviceErrorsKeepTheirStatus() throws Exception {
        willThrow(new ResponseStatusException(CONFLICT, "Only failed quizzes can be retried"))
                .given(quizzService).retry(userId, "a@a.com", quizzId);

        mockMvc.perform(post("/api/v1/quizzes/" + quizzId + "/retry")
                        .with(jwt().jwt(j -> j.subject(userId.toString()).claim("email", "a@a.com"))))
                .andExpect(status().isConflict());
    }

    @Test
    void withoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/quizzes/" + quizzId + "/retry")).andExpect(status().isUnauthorized());
    }
}
