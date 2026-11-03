package com.marcos.aiservice.quizz;

import java.util.UUID;

public record QuizzRequestedEvent(
        UUID quizzId,
        UUID fileId,
        Strategy strategy,
        int questions,
        int options,
        Difficulty difficulty
) {}