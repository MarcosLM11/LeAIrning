package com.marcos.ingestionservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class DocumentChunkerTests {

    @Test
    void splitsLongTextAndKeepsMetadata() {
        var text = "This is a sentence about ingestion. ".repeat(600);
        var chunks = new DocumentChunker().chunk(List.of(new Document(text, Map.of("fileId", "f1"))));
        assertTrue(chunks.size() > 1);
        chunks.forEach(c -> assertEquals("f1", c.getMetadata().get("fileId")));
    }
}
