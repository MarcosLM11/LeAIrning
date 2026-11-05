package com.marcos.aiservice.quizz;

import java.util.List;

public record AttemptResult(
        int score,
        int total,
        List<Correction> corrections
) {
    /** The right option and why, one per question in question order. */
    public record Correction(int correctIndex, String explanation) {}
}
