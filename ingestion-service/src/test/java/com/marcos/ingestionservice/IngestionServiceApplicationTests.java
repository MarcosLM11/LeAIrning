package com.marcos.ingestionservice;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@Import({TestcontainersConfiguration.class, FakeEmbeddingConfiguration.class})
@ActiveProfiles("test")
@SpringBootTest
class IngestionServiceApplicationTests {

    @Autowired S3Client s3;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired JdbcTemplate jdbc;
    @Value("${s3.bucket}") String bucket;

    @Test
    void uploadedMarkdownEndsUpChunkedInVectorStore() throws Exception {
        var fileId = UUID.randomUUID();
        s3.createBucket(b -> b.bucket(bucket));
        s3.putObject(r -> r.bucket(bucket).key(fileId.toString()),
                RequestBody.fromString("# Title\n\nhello world\n\n## Other\n\nmore text\n"));

        kafka.send("file.uploaded", fileId.toString(), """
                {"fileId":"%s","userId":"user","name":"notes.md","contentType":"text/markdown","size":10,\
                "uploadedAt":"2026-01-01T00:00:00Z"}""".formatted(fileId));

        await().atMost(30, SECONDS).until(() -> !jdbc.queryForList(
                "select 1 from vector_store where metadata->>'fileId' = ?", fileId.toString()).isEmpty());
    }
}
