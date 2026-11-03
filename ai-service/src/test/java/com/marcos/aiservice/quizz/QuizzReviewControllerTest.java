package com.marcos.aiservice.quizz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizzReviewControllerTest {

    private final QuizzRepository repository = mock(QuizzRepository.class);
    private final QuizzReviewController controller = new QuizzReviewController(repository);
    private final UUID userId = UUID.randomUUID();

    private Quizz quizz(Instant createdAt) {
        var quizz = new Quizz(UUID.randomUUID(), userId, Strategy.MAP);
        ReflectionTestUtils.setField(quizz, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(quizz, "createdAt", createdAt);
        return quizz;
    }

    @Test
    void listShowsNewestFirst() {
        var old = quizz(Instant.parse("2026-01-01T00:00:00Z"));
        var recent = quizz(Instant.parse("2026-02-01T00:00:00Z"));
        when(repository.findByUserId(userId)).thenReturn(List.of(old, recent));

        assertThat(controller.list(userId)).extracting(QuizzReviewController.Summary::id)
                .containsExactly(recent.getId(), old.getId());
    }

    @Test
    void reviewIncludesCorrectAnswerAndHidesOtherUsersQuizz() {
        var quizz = quizz(Instant.now());
        quizz.getQuestions().add(new Question("q?", List.of("a", "b"), 1, "because"));
        when(repository.findById(quizz.getId())).thenReturn(Optional.of(quizz));

        var review = controller.review(userId, quizz.getId());

        assertThat(review.questions()).singleElement().satisfies(q -> {
            assertThat(q.correctIndex()).isEqualTo(1);
            assertThat(q.explanation()).isEqualTo("because");
        });
        assertThatThrownBy(() -> controller.review(UUID.randomUUID(), quizz.getId())).hasMessageContaining("404");
    }
}
