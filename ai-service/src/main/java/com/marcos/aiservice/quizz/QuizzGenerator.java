package com.marcos.aiservice.quizz;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class QuizzGenerator {
    private static final int SECTION_CHUNKS = 20;

    private static final String QUESTIONS_PROMPT = """
            Genera {n} preguntas de dificultad {difficulty}, con {options} opciones cada una.

            Texto:
            {text}
            """;

    private final ChatClient quizzChatClient;
    private final JdbcClient jdbc;

    /** Splits the document into sections and asks for a share of the questions from each one. */
    GeneratedQuizz generate(UUID fileId, int questions, int options, Difficulty difficulty) {
        var chunks = chunks(fileId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No content for file " + fileId);
        }
        var sections = sections(chunks, questions);
        var all = new ArrayList<GeneratedQuestion>();
        for (int i = 0; i < sections.size(); i++) {
            all.addAll(ask(sections.get(i), share(questions, sections.size(), i), options, difficulty));
        }
        if (all.isEmpty()) {
            throw new IllegalStateException("The model returned no valid questions");
        }
        return new GeneratedQuizz(all);
    }

    static int share(int questions, int sections, int i) {
        return questions / sections + (i < questions % sections ? 1 : 0);
    }

    private List<GeneratedQuestion> ask(String text, int n, int options, Difficulty difficulty) {
        var quizz = quizzChatClient.prompt()
                .user(u -> u.text(QUESTIONS_PROMPT)
                        .param("n", n)
                        .param("difficulty", difficulty)
                        .param("options", options)
                        .param("text", text))
                .call()
                .entity(GeneratedQuizz.class, spec -> spec.useProviderStructuredOutput().validateSchema());
        return onlyValid(quizz.questions(), options);
    }

    static List<GeneratedQuestion> onlyValid(List<GeneratedQuestion> questions, int options) {
        return questions.stream()
                .filter(q -> q.options().size() == options && q.correctIndex() >= 0 && q.correctIndex() < options)
                .toList();
    }

    private List<String> chunks(UUID fileId) {
        return jdbc.sql("""
                        select content from vector_store
                        where metadata->>'fileId' = :fileId
                        order by coalesce((metadata->>'chunkIndex')::int, 0)
                        """)
                .param("fileId", fileId.toString())
                .query(String.class)
                .list();
    }

    /** Groups chunks into sections and keeps at most {@code max}, evenly spaced so the whole document is covered. */
    static List<String> sections(List<String> chunks, int max) {
        var all = IntStream.range(0, (chunks.size() + SECTION_CHUNKS - 1) / SECTION_CHUNKS)
                .mapToObj(i -> String.join("\n\n", chunks.subList(i * SECTION_CHUNKS, Math.min(chunks.size(), (i + 1) * SECTION_CHUNKS))))
                .toList();
        if (all.size() <= max) {
            return all;
        }
        return IntStream.range(0, max).mapToObj(i -> all.get((int) ((long) i * all.size() / max))).toList();
    }
}
