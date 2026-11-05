package com.marcos.aiservice.quizz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.marcos.aiservice.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AttemptRepositoryTest {

    @Autowired
    private AttemptRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void savesAnswersAsJsonArray() {
        var id = repository.saveAndFlush(new Attempt(UUID.randomUUID(), UUID.randomUUID(), List.of(2, 0, 1), 1)).getId();

        var json = (String) em.getEntityManager()
                .createNativeQuery("select cast(answers as text) from attempt where id = :id")
                .setParameter("id", id).getSingleResult();
        em.clear();

        assertThat(json.replace(" ", "")).isEqualTo("[2,0,1]");
        assertThat(repository.findById(id).orElseThrow().getAnswers()).containsExactly(2, 0, 1);
    }

    @Test
    void latestScoreIsThatOfTheNewestAttemptPerQuizzOfTheUser() throws InterruptedException {
        var user = UUID.randomUUID();
        var quizzA = UUID.randomUUID();
        var quizzB = UUID.randomUUID();
        repository.saveAndFlush(new Attempt(quizzA, user, List.of(0), 1));
        Thread.sleep(10);
        repository.saveAndFlush(new Attempt(quizzA, user, List.of(1), 4));
        repository.saveAndFlush(new Attempt(quizzB, user, List.of(0), 2));
        repository.saveAndFlush(new Attempt(quizzA, UUID.randomUUID(), List.of(0), 9)); // another user's attempt

        var latest = repository.findLatestScores(user);

        assertThat(latest).extracting(AttemptRepository.LatestScore::getQuizzId, AttemptRepository.LatestScore::getScore)
                .containsExactlyInAnyOrder(tuple(quizzA, 4), tuple(quizzB, 2));
    }
}
