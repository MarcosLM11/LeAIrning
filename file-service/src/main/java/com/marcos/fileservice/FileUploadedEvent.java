package com.marcos.fileservice;

import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

@Builder
public record FileUploadedEvent(
        UUID fileId,
        String userId,
        String name,
        String contentType,
        long size,
        Instant uploadedAt
) {}