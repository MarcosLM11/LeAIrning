package com.marcos.aiservice.quizz;

import java.util.UUID;

public record QuizzRequestedEvent(
        UUID quizzId,
        UUID userId,
        UUID fileId,
        int questions,
        int options,
        Difficulty difficulty
) {}