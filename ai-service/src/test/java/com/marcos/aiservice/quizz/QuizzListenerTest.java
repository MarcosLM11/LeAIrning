package com.marcos.aiservice.quizz;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class QuizzListenerTest {

    private final QuizzGenerator generator = mock(QuizzGenerator.class);
    private final QuizzService service = mock(QuizzService.class);
    private final KafkaTemplate<String, EmailRequestedEvent> kafka = mock(KafkaTemplate.class);
    private final QuizzListener listener = new QuizzListener(generator, service, kafka);
    private final QuizzRequestedEvent event =
            new QuizzRequestedEvent(UUID.randomUUID(), UUID.randomUUID(), "a@a.com", UUID.randomUUID(), 5, 4, Difficulty.MEDIO);

    @Test
    void completesQuizzWithGeneratedQuestions() {
        var generated = new GeneratedQuizz(List.of(new GeneratedQuestion("q?", List.of("a", "b"), 0, "e")));
        when(generator.generate(event.userId(), event.fileId(), 5, 4, Difficulty.MEDIO)).thenReturn(generated);

        listener.onQuizzRequested(event);

        verify(service).complete(event.quizzId(), generated);
        verify(kafka).send(QuizzListener.EMAIL_TOPIC, event.quizzId().toString(),
                new EmailRequestedEvent("a@a.com", "Tu quizz está listo", "Tu quizz " + event.quizzId() + " ya se ha generado."));
    }

    @Test
    void marksQuizzFailedWhenGenerationThrows() {
        when(generator.generate(event.userId(), event.fileId(), 5, 4, Difficulty.MEDIO))
                .thenThrow(new IllegalStateException("boom"));

        listener.onQuizzRequested(event);

        verify(service).fail(event.quizzId());
        verifyNoInteractions(kafka);
    }
}
