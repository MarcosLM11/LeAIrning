package com.marcos.aiservice.quizz;

import static org.junit.jupiter.api.Assertions.assertFalse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Quality of generated quizzes: a judge reads the whole source document and grades every question.
 * Run: OPENAI_API_KEY=... ./mvnw test -Peval. Writes evals/results/quizz.json (summary)
 * and quizz-details.json (every question and verdict, to spot-check the judge by hand).
 */
@Tag("eval")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.ai.vectorstore.pgvector.initialize-schema=true",
        "management.otlp.metrics.export.enabled=false",
        "management.otlp.tracing.export.enabled=false",
        "management.otlp.logging.export.enabled=false"})
class QuizzEvalTest {

    private static final Path EVALS = Path.of("../evals");
    private static final int QUESTIONS = 10;
    private static final int OPTIONS = 4;
    // shortcut: same model family as the generator, so the judge may favour it; spot-check quizz-details.json.
    private static final String JUDGE_MODEL = "gpt-5-mini";
    // shortcut: 0.9 cosine is a guess for "same question"; tune it by reading the flagged pairs.
    private static final double DUPLICATE_SIMILARITY = 0.9;

    private static final String PROMPT = """
            You review a multiple-choice question generated from the document below. Be strict and literal.

            Document:
            %s

            Question: %s
            Options:
            %s
            Marked as correct: option %d

            Fill the fields:
            - supported: true if the document clearly supports the marked option as the correct answer.
            - uniqueCorrect: true if no other option is also correct according to the document.
            - distractorsPlausible: true if the wrong options are on topic and could tempt someone who did not read the document, not absurd or unrelated.
            - selfContained: true if the question can be understood without seeing the document (no "according to the text").
            - reason: one short sentence.
            """;

    record Verdict(boolean supported, boolean uniqueCorrect, boolean distractorsPlausible, boolean selfContained, String reason) {
        boolean allGood() {
            return supported && uniqueCorrect && distractorsPlausible && selfContained;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {
        @Bean
        @ServiceConnection
        PostgreSQLContainer pgvectorContainer() {
            return new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg16"));
        }
    }

    @Autowired QuizzGenerator generator;
    @Autowired VectorStore vectorStore;
    @Autowired EmbeddingModel embeddings;
    @Autowired ChatClient.Builder builder;
    @Value("${quizz.model}") String generatorModel;

    @Test
    void measuresQuizzes() throws Exception {
        var json = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
        var userId = UUID.randomUUID();

        // shortcut: same reader and default splitter as ingestion-service, copied; the chunk count below flags drift.
        int chunks = 0;
        var fileIds = new LinkedHashMap<String, UUID>();
        var texts = new LinkedHashMap<String, String>();
        try (var pdfs = Files.list(EVALS.resolve("corpus"))) {
            for (var pdf : pdfs.filter(p -> p.toString().endsWith(".pdf")).sorted().toList()) {
                var name = pdf.getFileName().toString();
                var docs = new PagePdfDocumentReader(new FileSystemResource(pdf)).read();
                var fileId = UUID.randomUUID();
                docs.forEach(d -> d.getMetadata().putAll(Map.of(
                        "fileId", fileId.toString(), "userId", userId.toString(), "filename", name)));
                var split = TokenTextSplitter.builder().build().split(docs);
                for (int i = 0; i < split.size(); i++) split.get(i).getMetadata().put("chunkIndex", i);
                vectorStore.add(split);
                chunks += split.size();
                fileIds.put(name, fileId);
                texts.put(name, docs.stream().map(Document::getText).collect(Collectors.joining("\n")));
            }
        }
        assertFalse(chunks == 0);
        var baseline = EVALS.resolve("results/retrieval.json");
        int baselineChunks = Files.exists(baseline) ? json.readTree(baseline.toFile()).get("chunks").asInt() : -1;

        var judge = builder
                .defaultOptions(OpenAiChatOptions.builder().model(JUDGE_MODEL).reasoningEffort("medium"))
                .build();

        int requested = 0, returned = 0, generationFailures = 0, duplicates = 0;
        int supported = 0, unique = 0, plausible = 0, selfContained = 0, allGood = 0;
        var positions = new int[OPTIONS];
        var details = new ArrayList<Map<String, Object>>();
        var perQuizz = new ArrayList<Map<String, Object>>();

        var pool = Executors.newFixedThreadPool(3);
        try {
            for (var doc : fileIds.keySet()) {
                for (var difficulty : Difficulty.values()) {
                    requested += QUESTIONS;
                    List<GeneratedQuestion> questions;
                    try {
                        questions = generator.generate(userId, fileIds.get(doc), QUESTIONS, OPTIONS, difficulty).questions();
                    } catch (RuntimeException e) {
                        generationFailures++;
                        perQuizz.add(Map.of("doc", doc, "difficulty", difficulty.name(), "returned", 0, "error", String.valueOf(e.getMessage())));
                        continue;
                    }
                    returned += questions.size();
                    var duplicated = duplicated(questions);
                    duplicates += duplicated.size();

                    var text = texts.get(doc);
                    var futures = questions.stream()
                            .map(q -> pool.submit(() -> grade(judge, text, q)))
                            .toList();
                    for (int i = 0; i < questions.size(); i++) {
                        var q = questions.get(i);
                        var v = futures.get(i).get();
                        positions[q.correctIndex()]++;
                        if (v.supported()) supported++;
                        if (v.uniqueCorrect()) unique++;
                        if (v.distractorsPlausible()) plausible++;
                        if (v.selfContained()) selfContained++;
                        if (v.allGood()) allGood++;
                        details.add(detail(doc, difficulty, q, v, duplicated.contains(i)));
                    }
                    perQuizz.add(Map.of("doc", doc, "difficulty", difficulty.name(), "returned", questions.size(), "duplicated", duplicated.size()));
                }
            }
        } finally {
            pool.shutdown();
        }

        var summary = new LinkedHashMap<String, Object>();
        summary.put("generatorModel", generatorModel);
        summary.put("judgeModel", JUDGE_MODEL);
        summary.put("chunks", chunks);
        summary.put("retrievalBaselineChunks", baselineChunks);
        summary.put("quizzesFailed", generationFailures);
        summary.put("questionsRequested", requested);
        summary.put("questionsReturned", returned);
        summary.put("deficit", ratio(requested - returned, requested));
        summary.put("supported", ratio(supported, returned));
        summary.put("uniqueCorrect", ratio(unique, returned));
        summary.put("distractorsPlausible", ratio(plausible, returned));
        summary.put("selfContained", ratio(selfContained, returned));
        summary.put("allFourGood", ratio(allGood, returned));
        summary.put("duplicated", ratio(duplicates, returned));
        summary.put("correctIndexCounts", positions);
        summary.put("perQuizz", perQuizz);

        Files.createDirectories(EVALS.resolve("results"));
        json.writeValue(EVALS.resolve("results/quizz.json").toFile(), summary);
        json.writeValue(EVALS.resolve("results/quizz-details.json").toFile(), details);
    }

    private static Verdict grade(ChatClient judge, String document, GeneratedQuestion q) {
        var options = new ArrayList<String>();
        for (int i = 0; i < q.options().size(); i++) options.add(i + ". " + q.options().get(i));
        return judge.prompt()
                .messages(new UserMessage(PROMPT.formatted(document, q.question(), String.join("\n", options), q.correctIndex())))
                .call()
                .entity(Verdict.class, spec -> spec.useProviderStructuredOutput().validateSchema());
    }

    /** Indexes of the questions that look like another question of the same quizz. */
    private HashSet<Integer> duplicated(List<GeneratedQuestion> questions) {
        var vectors = embeddings.embed(questions.stream().map(GeneratedQuestion::question).toList());
        var flagged = new HashSet<Integer>();
        for (int i = 0; i < vectors.size(); i++) {
            for (int j = i + 1; j < vectors.size(); j++) {
                if (cosine(vectors.get(i), vectors.get(j)) >= DUPLICATE_SIMILARITY) {
                    flagged.add(i);
                    flagged.add(j);
                }
            }
        }
        return flagged;
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / Math.sqrt(na * nb);
    }

    private static Map<String, Object> detail(String doc, Difficulty difficulty, GeneratedQuestion q, Verdict v, boolean duplicated) {
        var m = new LinkedHashMap<String, Object>();
        m.put("doc", doc);
        m.put("difficulty", difficulty.name());
        m.put("question", q.question());
        m.put("options", q.options());
        m.put("correctIndex", q.correctIndex());
        m.put("explanation", q.explanation());
        m.put("duplicated", duplicated);
        m.put("supported", v.supported());
        m.put("uniqueCorrect", v.uniqueCorrect());
        m.put("distractorsPlausible", v.distractorsPlausible());
        m.put("selfContained", v.selfContained());
        m.put("reason", v.reason());
        return m;
    }

    private static double ratio(int part, int total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 1000.0;
    }
}
