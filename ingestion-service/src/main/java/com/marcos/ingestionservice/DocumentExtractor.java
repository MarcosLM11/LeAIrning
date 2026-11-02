package com.marcos.ingestionservice;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

@Component
class DocumentExtractor {

    List<Document> extract(FileType type, InputStream in, FileUploadedEvent event) {
        var docs = type.reader(new InputStreamResource(in) {
            @Override
            public String getFilename() {
                return event.name();
            }
        }).read();
        docs.forEach(d -> d.getMetadata().putAll(Map.of(
                "fileId", event.fileId().toString(),
                "userId", event.userId(),
                "filename", event.name()))
        );
        return docs;
    }
}
