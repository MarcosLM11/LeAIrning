package com.marcos.ingestionservice;

import java.time.Instant;
import java.util.UUID;

public record FileUploadedEvent(
        UUID fileId,
        String userId,
        String name,
        String contentType,
        long size,
        Instant uploadedAt
) {}