package com.marcos.aiservice.quizz;

import java.util.List;
import jakarta.validation.constraints.NotNull;

/** One chosen option index per question, in question order. */
public record AttemptRequest(@NotNull List<@NotNull Integer> answers) {}
