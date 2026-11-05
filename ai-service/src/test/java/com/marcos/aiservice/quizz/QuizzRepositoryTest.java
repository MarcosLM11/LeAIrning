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
    void savesQuestionsInOrderAsJson() {
        var quizz = new Quizz(UUID.randomUUID(), UUID.randomUUID(), 2, 3, Difficulty.MEDIO);
        quizz.setQuestions(List.of(
                new GeneratedQuestion("first?", List.of("a", "b", "c"), 1, "because"),
                new GeneratedQuestion("second?", List.of("x", "y"), 0, null)));
        var id = repository.saveAndFlush(quizz).getId();
        em.clear();

        var found = repository.findById(id).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(QuizzStatus.PENDING);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getQuestions()).extracting(GeneratedQuestion::question).containsExactly("first?", "second?");
        assertThat(found.getQuestions().getFirst().options()).containsExactly("a", "b", "c");
    }
}
