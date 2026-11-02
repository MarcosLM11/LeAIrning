package com.marcos.aiservice.quizz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.simple.JdbcClient;

class QuizzServiceTest {

    private final ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    private final JdbcClient jdbc = mock(JdbcClient.class, Answers.RETURNS_DEEP_STUBS);
    private final QuizzService service = new QuizzService(chatClient, jdbc);
    private final UUID fileId = UUID.randomUUID();

    @Test
    void rejectsOutOfRangeParams() {
        assertThatThrownBy(() -> service.generate(fileId, Strategy.MAP, 0, 4, Difficulty.MEDIO))
                .hasMessageContaining("400");
        assertThatThrownBy(() -> service.generate(fileId, Strategy.MAP, 10, 7, Difficulty.MEDIO))
                .hasMessageContaining("400");
    }

    @Test
    void unknownFileIs404() {
        when(jdbc.sql(anyString()).param(anyString(), any()).query(String.class).list()).thenReturn(List.of());

        assertThatThrownBy(() -> service.generate(fileId, Strategy.MAP, 10, 4, Difficulty.MEDIO))
                .hasMessageContaining("404");
    }

    @Test
    void failsWith502WhenModelReturnsNoValidQuestions() {
        when(jdbc.sql(anyString()).param(anyString(), any()).query(String.class).list()).thenReturn(List.of("texto"));
        when(chatClient.prompt().user(any(Consumer.class)).call().entity(eq(GeneratedQuizz.class), any(Consumer.class)))
                .thenReturn(new GeneratedQuizz(List.of(new GeneratedQuestion("q", List.of("a"), 5, "e"))));

        assertThatThrownBy(() -> service.generate(fileId, Strategy.FULL, 10, 4, Difficulty.MEDIO))
                .hasMessageContaining("502");
    }

    @Test
    void shareSumsToTotalAndEverySectionGetsOne() {
        var shares = IntStream.range(0, 7).map(i -> QuizzService.share(10, 7, i)).toArray();

        assertThat(IntStream.of(shares).sum()).isEqualTo(10);
        assertThat(IntStream.of(shares).min().getAsInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void onlyValidDropsBadQuestions() {
        var good = new GeneratedQuestion("q", List.of("a", "b", "c"), 2, "e");
        var badIndex = new GeneratedQuestion("q", List.of("a", "b", "c"), 3, "e");
        var badSize = new GeneratedQuestion("q", List.of("a", "b"), 0, "e");

        assertThat(QuizzService.onlyValid(List.of(good, badIndex, badSize), 3)).containsExactly(good);
    }

    @Test
    void sectionsKeepEverythingWhenUnderLimit() {
        var chunks = IntStream.range(0, 45).mapToObj(i -> "c" + i).toList();

        assertThat(QuizzService.sections(chunks, 10)).hasSize(3);
    }

    @Test
    void sectionsAreEvenlySpacedWhenOverLimit() {
        var chunks = IntStream.range(0, 400).mapToObj(i -> "c" + i).toList(); // 20 sections

        var sections = QuizzService.sections(chunks, 4);

        assertThat(sections).hasSize(4);
        assertThat(sections.get(0)).startsWith("c0\n");
        assertThat(sections.get(3)).startsWith("c300\n"); // sections 0, 5, 10, 15
    }
}
