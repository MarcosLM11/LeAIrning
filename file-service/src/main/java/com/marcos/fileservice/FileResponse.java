package com.marcos.fileservice;

import java.time.Instant;
import java.util.UUID;

record FileResponse(
        UUID id,
        String name,
        String contentType,
        long size,
        FileStatus status,
        Instant uploadedAt
) {

    static FileResponse from(FileMetadata f) {
        return new FileResponse(f.getId(), f.getName(), f.getContentType(), f.getSize(), f.getStatus(), f.getUploadedAt());
    }
}
