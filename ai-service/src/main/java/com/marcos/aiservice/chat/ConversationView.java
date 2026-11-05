package com.marcos.aiservice.chat;

import java.time.Instant;

public record ConversationView(
        String id,
        String title,
        Instant createdAt
) {}