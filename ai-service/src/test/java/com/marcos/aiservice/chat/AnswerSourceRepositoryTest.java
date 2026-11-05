package com.marcos.aiservice.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.marcos.aiservice.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnswerSourceRepositoryTest {

    @Autowired
    private AnswerSourceRepository repository;

    private final UUID fileA = UUID.randomUUID();
    private final UUID fileB = UUID.randomUUID();

    private void save(String event, String session, UUID file, String name, Integer page) {
        repository.saveAndFlush(new AnswerSource(event, session, file, name, page));
    }

    @Test
    void findsByEventsOrderedByFilenameAndPage() {
        save("e1", "s1", fileA, "b.pdf", 2);
        save("e1", "s1", fileA, "a.pdf", 7);
        save("e1", "s1", fileA, "b.pdf", 1);
        save("e2", "s1", fileB, "c.md", null);
        save("e3", "s1", fileB, "other.md", null);

        var found = repository.findByEventIdInOrderByFilenameAscPageAsc(List.of("e1", "e2"));

        assertThat(found).extracting(AnswerSource::getFilename, AnswerSource::getPage)
                .containsExactly(tuple("a.pdf", 7), tuple("b.pdf", 1),
                        tuple("b.pdf", 2), tuple("c.md", null));
    }

    @Test
    void deletesBySessionAndByFile() {
        save("e1", "s1", fileA, "a.pdf", 1);
        save("e2", "s2", fileA, "a.pdf", 1);
        save("e3", "s2", fileB, "b.pdf", 1);

        repository.deleteBySessionId("s1");
        assertThat(repository.findAll()).extracting(AnswerSource::getEventId).containsExactlyInAnyOrder("e2", "e3");

        repository.deleteByFileId(fileA);
        assertThat(repository.findAll()).extracting(AnswerSource::getEventId).containsExactly("e3");
    }
}
