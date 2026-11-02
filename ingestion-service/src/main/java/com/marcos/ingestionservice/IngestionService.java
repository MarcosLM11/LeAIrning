package com.marcos.ingestionservice;

import com.marcos.ingestionservice.storage.S3Service;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class IngestionService {

    private final S3Service s3;
    private final DocumentExtractor extractor;
    private final DocumentChunker chunker;
    private final DocumentLoader loader;

    IngestionService(S3Service s3, DocumentExtractor extractor, DocumentChunker chunker, DocumentLoader loader) {
        this.s3 = s3;
        this.extractor = extractor;
        this.chunker = chunker;
        this.loader = loader;
    }

    void ingest(FileUploadedEvent event) throws IOException {
        var type = FileType.of(event);
        if (type == null) {
            log.warn("Unsupported file {} ({}), skipping", event.fileId(), event.contentType());
            return;
        }
        try (var in = s3.open(event.fileId().toString())) {
            var docs = extractor.extract(type, in, event);
            var chunks = chunker.chunk(docs);

            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i).getMetadata().put("chunkIndex", i);
            }

            loader.load(event, chunks);
            log.info("Ingested {}: {} documents -> {} chunks", event.name(), docs.size(), chunks.size());
        }
    }
}
