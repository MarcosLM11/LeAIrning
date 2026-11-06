package com.marcos.ingestionservice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Retrieval quality against evals/questions.jsonl, with real OpenAI embeddings.
 * Run: OPENAI_API_KEY=... ./mvnw test -Peval. Writes evals/results/retrieval.json.
 */
@Tag("eval")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "management.otlp.metrics.export.enabled=false",
        "management.otlp.tracing.export.enabled=false",
        "management.otlp.logging.export.enabled=false"})
class RetrievalEvalTest {

    private static final Path EVALS = Path.of("../evals");
    private static final int MAX_K = 10;
    private static final int PROD_TOP_K = 4;
    private static final double PROD_THRESHOLD = 0.3;

    @Autowired VectorStore vectorStore;
    @Autowired DocumentExtractor extractor;
    @Autowired DocumentChunker chunker;
    @Value("${spring.ai.openai.embedding.options.model}") String embeddingModel;

    private static String norm(String s) {
        return s.replaceAll("\\s+", " ").strip();
    }

    @Test
    void measuresRetrieval() throws Exception {
        var json = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
        var chunksByDoc = new LinkedHashMap<String, List<Document>>();

        try (var pdfs = Files.list(EVALS.resolve("corpus"))) {
            for (var pdf : pdfs.filter(p -> p.toString().endsWith(".pdf")).sorted().toList()) {
                var name = pdf.getFileName().toString();
                var event = new FileUploadedEvent(UUID.randomUUID(), "eval", name, "application/pdf", Files.size(pdf), Instant.now());
                List<Document> chunks;
                try (var in = Files.newInputStream(pdf)) {
                    chunks = chunker.chunk(extractor.extract(FileType.PDF, in, event));
                }
                for (int i = 0; i < chunks.size(); i++) chunks.get(i).getMetadata().put("chunkIndex", i);
                vectorStore.add(chunks);
                chunksByDoc.put(name, chunks);
            }
        }

        int totalChunks = chunksByDoc.values().stream().mapToInt(List::size).sum();
        assertFalse(chunksByDoc.isEmpty());

        var hits = new int[MAX_K + 1]; // hits[r] = questions whose first hit is at rank r
        double mrr = 0;
        int prodHits = 0, n = 0;
        var unreachable = new ArrayList<String>();
        var perQuestion = new ArrayList<Map<String, Object>>();

        for (var line : Files.readAllLines(EVALS.resolve("questions.jsonl"))) {
            var q = json.readTree(line);
            if (!"answerable".equals(q.get("type").asString())) continue;
            n++;
            var id = q.get("id").asString();
            var doc = q.get("doc").asString();
            var golds = new ArrayList<String>();
            q.get("gold_passages").forEach(g -> golds.add(norm(g.asString())));
            Predicate<String> answers = text -> golds.stream().anyMatch(norm(text)::contains);

            // A passage cut by a chunk boundary can never be retrieved whole: it counts as a miss.
            if (chunksByDoc.get(doc).stream().noneMatch(c -> answers.test(c.getText()))) unreachable.add(id);

            var found = vectorStore.similaritySearch(SearchRequest.builder().query(q.get("question").asString()).topK(MAX_K).build());
            int rank = 0;

            for (int i = 0; i < found.size() && rank == 0; i++) {
                var d = found.get(i);
                if (doc.equals(d.getMetadata().get("filename")) && answers.test(d.getText())) rank = i + 1;
            }

            if (rank > 0) {
                hits[rank]++;
                mrr += 1.0 / rank;
                var d = found.get(rank - 1);
                if (rank <= PROD_TOP_K && d.getScore() != null && d.getScore() >= PROD_THRESHOLD) prodHits++;
            }

            perQuestion.add(Map.of("id", id, "rank", rank));
        }

        var report = new LinkedHashMap<String, Object>();
        report.put("embeddingModel", embeddingModel);
        report.put("chunks", totalChunks);
        report.put("questions", n);
        for (int k : new int[]{1, 3, 5, MAX_K}) {
            report.put("recall@" + k, round(cumulative(hits, k) / (double) n));
        }
        report.put("mrr@" + MAX_K, round(mrr / n));
        report.put("recall@" + PROD_TOP_K + " threshold " + PROD_THRESHOLD, round(prodHits / (double) n));
        report.put("unreachable", unreachable);
        report.put("perQuestion", perQuestion);

        Files.createDirectories(EVALS.resolve("results"));
        json.writeValue(EVALS.resolve("results/retrieval.json").toFile(), report);
    }

    private static int cumulative(int[] hits, int k) {
        int sum = 0;
        for (int r = 1; r <= k; r++) sum += hits[r];
        return sum;
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}