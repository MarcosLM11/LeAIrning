package com.marcos.aiservice.chat;

import java.time.Instant;

public record MessageView(
        String role,
        String text,
        Instant createdAt
) {}