package com.marcos.aiservice.quizz;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class QuizzService {
    private static final int SECTION_CHUNKS = 20;
    private static final int MAX_CONCEPT_SECTIONS = 20;
    private static final int FULL_MAX_CHARS = 400_000;

    private static final String QUESTIONS_PROMPT = """
            Genera {n} preguntas de dificultad {difficulty} (FACIL, MEDIO o DIFICIL), cada una con {options} opciones.
            correctIndex es la posición (desde 0) de la opción correcta.

            Texto:
            {text}
            """;

    private static final String CONCEPTS_PROMPT = """
            Extrae los 3 conceptos más importantes de este fragmento. Cada uno con una explicación de 2 frases.

            Fragmento:
            {text}
            """;

    private final ChatClient quizzChatClient;
    private final JdbcClient jdbc;

    public GeneratedQuizz generate(UUID fileId, Strategy strategy, int questions, int options, Difficulty difficulty) {
        if (questions < 1 || questions > 50 || options < 2 || options > 6) {
            throw new ResponseStatusException(BAD_REQUEST, "questions must be 1-50 and options 2-6");
        }
        var chunks = chunks(fileId);
        if (chunks.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No content for file " + fileId);
        }
        var quizz = switch (strategy) {
            case FULL -> ask(truncate(String.join("\n\n", chunks)), questions, options, difficulty);
            case MAP -> map(chunks, questions, options, difficulty);
            case CONCEPTS -> concepts(chunks, questions, options, difficulty);
        };
        if (quizz.questions().isEmpty()) {
            throw new ResponseStatusException(BAD_GATEWAY, "The model returned no valid questions");
        }
        return quizz;
    }

    private GeneratedQuizz map(List<String> chunks, int questions, int options, Difficulty difficulty) {
        var sections = sections(chunks, questions);
        var all = new ArrayList<GeneratedQuestion>();
        for (int i = 0; i < sections.size(); i++) {
            all.addAll(ask(sections.get(i), share(questions, sections.size(), i), options, difficulty).questions());
        }
        return new GeneratedQuizz(all);
    }

    static int share(int questions, int sections, int i) {
        return questions / sections + (i < questions % sections ? 1 : 0);
    }

    private GeneratedQuizz concepts(List<String> chunks, int questions, int options, Difficulty difficulty) {
        var all = new ArrayList<String>();
        for (var section : sections(chunks, MAX_CONCEPT_SECTIONS)) {
            var found = quizzChatClient.prompt()
                    .user(u -> u.text(CONCEPTS_PROMPT).param("text", section))
                    .call()
                    .entity(Concepts.class, spec -> spec.useProviderStructuredOutput().validateSchema());
            all.addAll(found.concepts());
        }
        return ask(String.join("\n", all), questions, options, difficulty);
    }

    private GeneratedQuizz ask(String text, int n, int options, Difficulty difficulty) {
        var quizz = quizzChatClient.prompt()
                .user(u -> u.text(QUESTIONS_PROMPT)
                        .param("n", n)
                        .param("difficulty", difficulty)
                        .param("options", options)
                        .param("text", text))
                .call()
                .entity(GeneratedQuizz.class, spec -> spec.useProviderStructuredOutput().validateSchema());
        return new GeneratedQuizz(onlyValid(quizz.questions(), options));
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

    private static String truncate(String text) {
        return text.length() > FULL_MAX_CHARS ? text.substring(0, FULL_MAX_CHARS) : text;
    }
}
