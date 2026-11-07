package com.marcos.aiservice.quizz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
    GeneratedQuizz generate(UUID userId, UUID fileId, int questions, int options, Difficulty difficulty) {
        var chunks = chunks(userId, fileId);
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
                .entity(QuizzDraft.class, spec -> spec.useProviderStructuredOutput().validateSchema());
        return toValid(quizz.questions(), options, ThreadLocalRandom.current());
    }

    /** Keeps the questions with the right number of distinct options and shuffles each one, so the correct position is random. */
    static List<GeneratedQuestion> toValid(List<QuizzDraft.Question> questions, int options, Random random) {
        return questions.stream()
                .filter(q -> q.correctAnswer() != null && q.wrongAnswers() != null && q.wrongAnswers().size() == options - 1)
                .filter(q -> {
                    var all = new ArrayList<>(q.wrongAnswers());
                    all.add(q.correctAnswer());
                    return all.stream().noneMatch(Objects::isNull)
                            && all.stream().map(a -> a.strip().toLowerCase()).distinct().count() == options;
                })
                .map(q -> {
                    var shuffled = new ArrayList<>(q.wrongAnswers());
                    shuffled.add(q.correctAnswer());
                    Collections.shuffle(shuffled, random);
                    return new GeneratedQuestion(q.question(), shuffled, shuffled.indexOf(q.correctAnswer()), q.explanation());
                })
                .toList();
    }

    private List<String> chunks(UUID userId, UUID fileId) {
        return jdbc.sql("""
                        select content from vector_store
                        where metadata->>'fileId' = :fileId and metadata->>'userId' = :userId
                        order by coalesce((metadata->>'chunkIndex')::int, 0)
                        """)
                .param("fileId", fileId.toString())
                .param("userId", userId.toString())
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
