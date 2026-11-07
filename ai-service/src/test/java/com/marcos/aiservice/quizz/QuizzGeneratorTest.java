package com.marcos.aiservice.quizz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.simple.JdbcClient;

class QuizzGeneratorTest {

    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final JdbcClient jdbc = mock(JdbcClient.class, Answers.RETURNS_DEEP_STUBS);
    private final QuizzGenerator generator = new QuizzGenerator(chatClient, jdbc);
    private final UUID userId = UUID.randomUUID();
    private final UUID fileId = UUID.randomUUID();

    @Test
    void failsWhenFileHasNoContent() {
        when(jdbc.sql(anyString()).param(anyString(), any()).param(anyString(), any()).query(String.class).list()).thenReturn(List.of());

        assertThatThrownBy(() -> generator.generate(userId, fileId, 10, 4, Difficulty.MEDIO))
                .hasMessageContaining("No content");
    }

    @Test
    void failsWhenModelReturnsNoValidQuestions() {
        when(jdbc.sql(anyString()).param(anyString(), any()).param(anyString(), any()).query(String.class).list()).thenReturn(List.of("texto"));
        when(chatClient.prompt().user(any(Consumer.class)).call().entity(eq(QuizzDraft.class), any(Consumer.class)))
                .thenReturn(new QuizzDraft(List.of(new QuizzDraft.Question("q", "a", List.of("b"), "e"))));

        assertThatThrownBy(() -> generator.generate(userId, fileId, 10, 4, Difficulty.MEDIO))
                .hasMessageContaining("no valid questions");
    }

    @Test
    void shareSumsToTotalAndEverySectionGetsOne() {
        var shares = IntStream.range(0, 7).map(i -> QuizzGenerator.share(10, 7, i)).toArray();

        assertThat(IntStream.of(shares).sum()).isEqualTo(10);
        assertThat(IntStream.of(shares).min().getAsInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void toValidDropsQuestionsWithTheWrongNumberOfOrRepeatedOptions() {
        var good = new QuizzDraft.Question("q", "a", List.of("b", "c"), "e");
        var tooFew = new QuizzDraft.Question("q", "a", List.of("b"), "e");
        var repeated = new QuizzDraft.Question("q", "a", List.of("b", "b"), "e");
        var sameAsCorrect = new QuizzDraft.Question("q", "a", List.of(" A ", "c"), "e");

        var valid = QuizzGenerator.toValid(List.of(good, tooFew, repeated, sameAsCorrect), 3, new Random(1));

        assertThat(valid).extracting(GeneratedQuestion::question).hasSize(1);
        assertThat(valid.getFirst().options()).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void toValidMarksTheCorrectAnswerAndSpreadsItsPosition() {
        var drafts = IntStream.range(0, 400)
                .mapToObj(i -> new QuizzDraft.Question("q" + i, "right" + i, List.of("w1", "w2", "w3"), "e"))
                .toList();

        var valid = QuizzGenerator.toValid(drafts, 4, new Random(7));

        assertThat(valid).allSatisfy(q -> assertThat(q.options().get(q.correctIndex())).startsWith("right"));
        assertThat(valid.stream().collect(Collectors.groupingBy(GeneratedQuestion::correctIndex, Collectors.counting())))
                .hasSize(4).allSatisfy((_, count) -> assertThat(count).isBetween(70L, 130L));
    }

    @Test
    void sectionsKeepEverythingWhenUnderLimit() {
        var chunks = IntStream.range(0, 45).mapToObj(i -> "c" + i).toList();

        assertThat(QuizzGenerator.sections(chunks, 10)).hasSize(3);
    }

    @Test
    void sectionsAreEvenlySpacedWhenOverLimit() {
        var chunks = IntStream.range(0, 400).mapToObj(i -> "c" + i).toList(); // 20 sections

        var sections = QuizzGenerator.sections(chunks, 4);

        assertThat(sections).hasSize(4);
        assertThat(sections.get(0)).startsWith("c0\n");
        assertThat(sections.get(3)).startsWith("c300\n"); // sections 0, 5, 10, 15
    }
}
