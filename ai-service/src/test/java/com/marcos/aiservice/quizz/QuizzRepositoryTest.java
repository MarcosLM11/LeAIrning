package com.marcos.aiservice.quizz;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcos.aiservice.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuizzRepositoryTest {

    @Autowired
    private QuizzRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void savesQuestionsInOrderWithOptionsAsJson() {
        var userId = UUID.randomUUID();
        var quizz = new Quizz(UUID.randomUUID(), userId, Strategy.MAP);
        quizz.getQuestions().add(new Question("first?", List.of("a", "b", "c"), 1, "because"));
        quizz.getQuestions().add(new Question("second?", List.of("x", "y"), 0, null));
        var id = repository.saveAndFlush(quizz).getId();
        em.clear();

        var found = repository.findByUserId(userId);

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getId()).isEqualTo(id);
        assertThat(found.getFirst().getStatus()).isEqualTo(QuizzStatus.PENDING);
        assertThat(found.getFirst().getCreatedAt()).isNotNull();
        assertThat(found.getFirst().getQuestions()).extracting(Question::getText).containsExactly("first?", "second?");
        assertThat(found.getFirst().getQuestions().getFirst().getOptions()).containsExactly("a", "b", "c");
    }
}
