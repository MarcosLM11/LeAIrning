package com.marcos.aiservice.quizz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.eq;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class QuizzServiceTest {

    private final QuizzRepository repository = mock(QuizzRepository.class);
    private final AttemptRepository attempts = mock(AttemptRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, QuizzRequestedEvent> kafka = mock(KafkaTemplate.class);
    private final QuizzService service = new QuizzService(repository, attempts, kafka);
    private final UUID userId = UUID.randomUUID();
    private final UUID fileId = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    private Quizz pendingQuizz() {
        var quizz = new Quizz(fileId, userId, 5, 3, Difficulty.FACIL);
        ReflectionTestUtils.setField(quizz, "id", id);
        when(repository.findById(id)).thenReturn(Optional.of(quizz));
        return quizz;
    }

    @Test
    void startRejectsOutOfRangeParams() {
        assertThatThrownBy(() -> service.start(userId, "a@a.com", fileId, 0, 4, Difficulty.MEDIO))
                .hasMessageContaining("400");
        assertThatThrownBy(() -> service.start(userId, "a@a.com", fileId, 10, 7, Difficulty.MEDIO))
                .hasMessageContaining("400");
    }

    @Test
    void startSavesPendingQuizzAndSendsEvent() {
        when(repository.save(any(Quizz.class))).thenAnswer(call -> {
            var saved = call.<Quizz>getArgument(0);
            ReflectionTestUtils.setField(saved, "id", id);
            return saved;
        });

        var result = service.start(userId, "a@a.com", fileId, 5, 3, Difficulty.FACIL);

        assertThat(result).isEqualTo(id);
        verify(kafka).send(QuizzService.TOPIC, id.toString(),
                new QuizzRequestedEvent(id, userId, "a@a.com", fileId, 5, 3, Difficulty.FACIL));
    }

    @Test
    void listReturnsSummariesOfTheUser() {
        var quizz = pendingQuizz();
        when(repository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(quizz)));

        assertThat(service.list(userId, PageRequest.of(0, 20))).extracting(QuizzSummary::id, QuizzSummary::questions, QuizzSummary::difficulty)
                .containsExactly(tuple(id, 5, Difficulty.FACIL));
    }

    private Quizz readyQuizz() {
        var quizz = pendingQuizz();
        quizz.setQuestions(List.of(
                new GeneratedQuestion("q1", List.of("a", "b", "c"), 1, "why1"),
                new GeneratedQuestion("q2", List.of("a", "b", "c"), 2, "why2")));
        quizz.setStatus(QuizzStatus.READY);
        return quizz;
    }

    @Test
    void retryRelaunchesOnlyFailedQuizzes() {
        var quizz = pendingQuizz();
        assertThatThrownBy(() -> service.retry(userId, "a@a.com", id)).hasMessageContaining("409");

        quizz.setStatus(QuizzStatus.FAILED);
        service.retry(userId, "a@a.com", id);

        assertThat(quizz.getStatus()).isEqualTo(QuizzStatus.PENDING);
        verify(repository).save(quizz);
        verify(kafka).send(QuizzService.TOPIC, id.toString(),
                new QuizzRequestedEvent(id, userId, "a@a.com", fileId, 5, 3, Difficulty.FACIL));
    }

    @Test
    void submitGradesOnServerAndStoresAttempt() {
        readyQuizz();

        var result = service.submit(userId, id, List.of(1, 0));

        assertThat(result.score()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.corrections()).extracting(AttemptResult.Correction::correctIndex).containsExactly(1, 2);
        verify(attempts).save(any(Attempt.class));
    }

    @Test
    void submitRejectsWrongShapeOrNotReady() {
        pendingQuizz();
        assertThatThrownBy(() -> service.submit(userId, id, List.of(0))).hasMessageContaining("409");
        readyQuizz();
        assertThatThrownBy(() -> service.submit(userId, id, List.of(0))).hasMessageContaining("400");
        assertThatThrownBy(() -> service.submit(userId, id, List.of(0, 3))).hasMessageContaining("400");
        assertThatThrownBy(() -> service.submit(UUID.randomUUID(), id, List.of(0, 0))).hasMessageContaining("404");
    }

    @Test
    void listCountsGeneratedQuestionsOnceReady() {
        var quizz = readyQuizz(); // requested 5, the model produced 2
        when(repository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(quizz)));

        assertThat(service.list(userId, PageRequest.of(0, 20))).extracting(QuizzSummary::questions).containsExactly(2);
    }

    @Test
    void listShowsLatestScore() {
        var quizz = pendingQuizz();
        when(repository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(quizz)));
        var latest = mock(AttemptRepository.LatestScore.class);
        when(latest.getQuizzId()).thenReturn(id);
        when(latest.getScore()).thenReturn(3);
        when(attempts.findLatestScores(userId)).thenReturn(List.of(latest));

        assertThat(service.list(userId, PageRequest.of(0, 20))).extracting(QuizzSummary::score).containsExactly(3);
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
        assertThat(quizz.getQuestions()).extracting(GeneratedQuestion::question).containsExactly("q?");
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
