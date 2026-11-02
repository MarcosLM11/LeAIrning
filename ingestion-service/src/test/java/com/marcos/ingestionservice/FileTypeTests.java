package com.marcos.ingestionservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileTypeTests {

    private static FileType of(String name, String contentType) {
        return FileType.of(new FileUploadedEvent(UUID.randomUUID(), "u", name, contentType, 1, Instant.now()));
    }

    @Test
    void detectsByExtensionOrContentType() {
        assertEquals(FileType.PDF, of("a.PDF", "application/octet-stream"));
        assertEquals(FileType.PDF, of("a", "application/pdf"));
        assertEquals(FileType.MARKDOWN, of("a.md", "application/octet-stream"));
        assertNull(of("a.exe", "application/octet-stream"));
    }
}
