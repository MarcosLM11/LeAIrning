package com.marcos.aiservice.quizz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class QuizzServiceTest {

    private final QuizzRepository repository = mock(QuizzRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, QuizzRequestedEvent> kafka = mock(KafkaTemplate.class);
    private final QuizzService service = new QuizzService(repository, kafka);
    private final UUID userId = UUID.randomUUID();
    private final UUID fileId = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    private Quizz pendingQuizz() {
        var quizz = new Quizz(fileId, userId, Strategy.MAP);
        ReflectionTestUtils.setField(quizz, "id", id);
        when(repository.findById(id)).thenReturn(Optional.of(quizz));
        return quizz;
    }

    @Test
    void startRejectsOutOfRangeParams() {
        assertThatThrownBy(() -> service.start(userId, fileId, Strategy.MAP, 0, 4, Difficulty.MEDIO))
                .hasMessageContaining("400");
        assertThatThrownBy(() -> service.start(userId, fileId, Strategy.MAP, 10, 7, Difficulty.MEDIO))
                .hasMessageContaining("400");
    }

    @Test
    void startSavesPendingQuizzAndSendsEvent() {
        when(repository.save(any(Quizz.class))).thenAnswer(call -> {
            var saved = call.<Quizz>getArgument(0);
            ReflectionTestUtils.setField(saved, "id", id);
            return saved;
        });

        var result = service.start(userId, fileId, Strategy.CONCEPTS, 5, 3, Difficulty.FACIL);

        assertThat(result).isEqualTo(id);
        verify(kafka).send(QuizzService.TOPIC, id.toString(),
                new QuizzRequestedEvent(id, fileId, Strategy.CONCEPTS, 5, 3, Difficulty.FACIL));
    }

    @Test
    void getHidesQuizzOfAnotherUser() {
        pendingQuizz();

        assertThatThrownBy(() -> service.get(UUID.randomUUID(), id)).hasMessageContaining("404");
    }

    @Test
    void completeStoresQuestionsAndMarksReady() {
        var quizz = pendingQuizz();

        service.complete(id, new GeneratedQuizz(List.of(new GeneratedQuestion("q?", List.of("a", "b"), 1, "e"))));

        assertThat(quizz.getStatus()).isEqualTo(QuizzStatus.READY);
        assertThat(quizz.getQuestions()).extracting(Question::getText).containsExactly("q?");
    }

    @Test
    void failMarksFailedButNeverOverwritesAFinishedQuizz() {
        var quizz = pendingQuizz();
        service.fail(id);
        assertThat(quizz.getStatus()).isEqualTo(QuizzStatus.FAILED);

        service.complete(id, new GeneratedQuizz(List.of(new GeneratedQuestion("q?", List.of("a", "b"), 1, "e"))));

        assertThat(quizz.getStatus()).isEqualTo(QuizzStatus.FAILED);
        assertThat(quizz.getQuestions()).isEmpty();
    }
}
