package com.marcos.aiservice.quizz;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuizzListenerTest {

    private final QuizzGenerator generator = mock(QuizzGenerator.class);
    private final QuizzService service = mock(QuizzService.class);
    private final QuizzListener listener = new QuizzListener(generator, service);
    private final QuizzRequestedEvent event =
            new QuizzRequestedEvent(UUID.randomUUID(), UUID.randomUUID(), 5, 4, Difficulty.MEDIO);

    @Test
    void completesQuizzWithGeneratedQuestions() {
        var generated = new GeneratedQuizz(List.of(new GeneratedQuestion("q?", List.of("a", "b"), 0, "e")));
        when(generator.generate(event.fileId(), 5, 4, Difficulty.MEDIO)).thenReturn(generated);

        listener.onQuizzRequested(event);

        verify(service).complete(event.quizzId(), generated);
    }

    @Test
    void marksQuizzFailedWhenGenerationThrows() {
        when(generator.generate(event.fileId(), 5, 4, Difficulty.MEDIO))
                .thenThrow(new IllegalStateException("boom"));

        listener.onQuizzRequested(event);

        verify(service).fail(event.quizzId());
    }
}
