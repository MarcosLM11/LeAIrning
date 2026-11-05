package com.marcos.aiservice.quizz;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.marcos.aiservice.security.SecurityConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(QuizzController.class)
@Import(SecurityConfig.class)
class QuizzAttemptsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QuizzService quizzService;

    private final UUID userId = UUID.randomUUID();
    private final UUID quizzId = UUID.randomUUID();

    @Test
    void returnsScoreAndCorrections() throws Exception {
        given(quizzService.submit(userId, quizzId, List.of(1, 0))).willReturn(new AttemptResult(1, 2,
                List.of(new AttemptResult.Correction(1, "why"), new AttemptResult.Correction(2, null))));

        mockMvc.perform(post("/api/v1/quizz/" + quizzId + "/attempts").with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[1,0]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.corrections[0].correctIndex").value(1))
                .andExpect(jsonPath("$.corrections[0].explanation").value("why"))
                .andExpect(jsonPath("$.corrections[1].correctIndex").value(2));
    }

    @Test
    void serviceErrorsKeepTheirStatus() throws Exception {
        given(quizzService.submit(userId, quizzId, List.of(0)))
                .willThrow(new ResponseStatusException(CONFLICT, "Quizz is not ready"));

        mockMvc.perform(post("/api/v1/quizz/" + quizzId + "/attempts").with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[0]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void withoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/quizz/" + quizzId + "/attempts")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[0]}"))
                .andExpect(status().isUnauthorized());
    }
}
