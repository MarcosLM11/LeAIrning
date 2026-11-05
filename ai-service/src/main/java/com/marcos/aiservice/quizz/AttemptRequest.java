package com.marcos.aiservice.quizz;

import java.util.List;

/** One chosen option index per question, in question order. */
public record AttemptRequest(List<Integer> answers) {}
