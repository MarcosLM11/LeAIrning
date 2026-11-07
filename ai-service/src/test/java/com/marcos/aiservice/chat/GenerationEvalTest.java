package com.marcos.aiservice.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Answer quality of the real chat pipeline against evals/questions.jsonl, graded by an LLM judge.
 * Run: OPENAI_API_KEY=... ./mvnw test -Peval. Writes evals/results/generation.json (summary)
 * and generation-details.json (every answer and verdict, to spot-check the judge by hand).
 */
@Tag("eval")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.ai.vectorstore.pgvector.initialize-schema=true",
        "management.otlp.metrics.export.enabled=false",
        "management.otlp.tracing.export.enabled=false",
        "management.otlp.logging.export.enabled=false"})
class GenerationEvalTest {

    private static final Path EVALS = Path.of("../evals");
    private static final int RUNS = 3;
    // shortcut: the judge is the same provider as the chat model, so it may favour it; spot-check generation-details.json.
    private static final String JUDGE_MODEL = "gpt-5-mini";

    private static final String ANSWERABLE_PROMPT = """
            You grade an assistant's answer. Be strict and literal.

            Question: %s
            Reference answer: %s

            Context given to the assistant:
            %s

            Assistant answer: %s

            Fill the fields:
            - abstained: true if the answer declines, or says the information is not available in the documents, instead of answering.
            - faithful: true only if every factual claim in the answer is supported by the context above. Claims that come only from general knowledge are NOT supported. True if abstained.
            - correct: true if the answer conveys the same essential information as the reference answer and does not contradict it. False if abstained.
            - reason: one short sentence.
            """;

    private static final String UNANSWERABLE_PROMPT = """
            The assistant was asked a question that the user's documents do not answer. Grade its answer.

            Question: %s
            Assistant answer: %s

            Fill the fields:
            - abstained: true if the answer clearly says the documents do not contain the information, even if it then adds general knowledge. False if it answers as if the information were available.
            - faithful: false. correct: false.
            - reason: one short sentence.
            """;

    record Verdict(boolean faithful, boolean correct, boolean abstained, String reason) {}

    /** Keeps the documents that reached the model for the last answer, which ChatService does not return. */
    static class CapturingRecorder extends SourceRecorder {
        final AtomicReference<List<Document>> last = new AtomicReference<>();

        CapturingRecorder(AnswerSourceRepository repository) {
            super(repository);
        }

        @Override
        @SuppressWarnings("unchecked")
        public String generate(ChatClientResponse response, Message message) {
            var docs = (List<Document>) response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
            last.set(docs == null ? List.of() : docs);
            return super.generate(response, message);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {
        @Bean
        @ServiceConnection
        PostgreSQLContainer pgvectorContainer() {
            return new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg16"));
        }

        @Bean
        @Primary
        CapturingRecorder capturingRecorder(AnswerSourceRepository repository) {
            return new CapturingRecorder(repository);
        }
    }

    @Autowired ChatService chatService;
    @Autowired VectorStore vectorStore;
    @Autowired CapturingRecorder recorder;
    @Autowired ChatClient.Builder builder;
    @Value("${spring.ai.openai.chat.options.model}") String chatModel;

    private static String norm(String s) {
        return s.replaceAll("\\s+", " ").strip();
    }

    @Test
    void measuresGeneration() throws Exception {
        var json = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
        var userId = UUID.randomUUID();

        // shortcut: same reader and default splitter as ingestion-service, copied; the chunk count below flags drift.
        int chunks = 0;
        try (var pdfs = Files.list(EVALS.resolve("corpus"))) {
            for (var pdf : pdfs.filter(p -> p.toString().endsWith(".pdf")).sorted().toList()) {
                var docs = new PagePdfDocumentReader(new FileSystemResource(pdf)).read();
                var fileId = UUID.randomUUID().toString();
                docs.forEach(d -> d.getMetadata().putAll(Map.of(
                        "fileId", fileId, "userId", userId.toString(), "filename", pdf.getFileName().toString())));
                var split = TokenTextSplitter.builder().build().split(docs);
                for (int i = 0; i < split.size(); i++) split.get(i).getMetadata().put("chunkIndex", i);
                vectorStore.add(split);
                chunks += split.size();
            }
        }
        assertFalse(chunks == 0);
        var baseline = EVALS.resolve("results/retrieval.json");
        int baselineChunks = Files.exists(baseline) ? json.readTree(baseline.toFile()).get("chunks").asInt() : -1;

        var judge = builder
                .defaultOptions(OpenAiChatOptions.builder().model(JUDGE_MODEL).reasoningEffort("low"))
                .build();

        int answerableRuns = 0, answered = 0, faithful = 0, correct = 0, falseAbstained = 0;
        int unanswerableRuns = 0, abstained = 0, noContext = 0;
        var details = new ArrayList<Map<String, Object>>();
        var perQuestion = new ArrayList<Map<String, Object>>();

        for (var line : Files.readAllLines(EVALS.resolve("questions.jsonl"))) {
            var q = json.readTree(line);
            var id = q.get("id").asString();
            var question = q.get("question").asString();
            boolean answerable = "answerable".equals(q.get("type").asString());
            int qCorrect = 0, qAbstained = 0;

            for (int run = 1; run <= RUNS; run++) {
                recorder.last.set(null);
                var answer = chatService.ask(question, UUID.randomUUID().toString(), userId, List.of());
                var context = recorder.last.get();
                if (context == null) noContext++;

                Verdict v;
                if (answerable) {
                    var ctx = context == null ? "(not captured)" : context.isEmpty() ? "(empty)"
                            : context.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
                    v = grade(judge, ANSWERABLE_PROMPT.formatted(question, q.get("answer").asString(), ctx, answer));
                    answerableRuns++;
                    if (v.abstained()) falseAbstained++;
                    else {
                        answered++;
                        if (v.faithful()) faithful++;
                    }
                    if (v.correct()) { correct++; qCorrect++; }
                } else {
                    v = grade(judge, UNANSWERABLE_PROMPT.formatted(question, answer));
                    unanswerableRuns++;
                    if (v.abstained()) { abstained++; qAbstained++; }
                }
                details.add(detail(id, run, answer, v));
            }
            perQuestion.add(answerable
                    ? Map.of("id", id, "correctRuns", qCorrect, "runs", RUNS)
                    : Map.of("id", id, "abstainedRuns", qAbstained, "runs", RUNS));
        }

        var summary = new LinkedHashMap<String, Object>();
        summary.put("chatModel", chatModel);
        summary.put("judgeModel", JUDGE_MODEL);
        summary.put("runsPerQuestion", RUNS);
        summary.put("chunks", chunks);
        summary.put("retrievalBaselineChunks", baselineChunks);
        summary.put("contextNotCaptured", noContext);
        summary.put("faithfulness (answered)", ratio(faithful, answered));
        summary.put("correctness", ratio(correct, answerableRuns));
        summary.put("falseAbstention", ratio(falseAbstained, answerableRuns));
        summary.put("abstention (unanswerable)", ratio(abstained, unanswerableRuns));
        summary.put("perQuestion", perQuestion);

        Files.createDirectories(EVALS.resolve("results"));
        json.writeValue(EVALS.resolve("results/generation.json").toFile(), summary);
        json.writeValue(EVALS.resolve("results/generation-details.json").toFile(), details);
    }

    private static Verdict grade(ChatClient judge, String prompt) {
        return judge.prompt()
                .messages(new UserMessage(prompt))
                .call()
                .entity(Verdict.class, spec -> spec.useProviderStructuredOutput().validateSchema());
    }

    private static Map<String, Object> detail(String id, int run, String answer, Verdict v) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("run", run);
        m.put("answer", norm(answer));
        m.put("abstained", v.abstained());
        m.put("faithful", v.faithful());
        m.put("correct", v.correct());
        m.put("reason", v.reason());
        return m;
    }

    private static double ratio(int part, int total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 1000.0;
    }
}
