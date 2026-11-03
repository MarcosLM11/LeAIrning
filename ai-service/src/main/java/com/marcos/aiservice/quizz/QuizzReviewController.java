package com.marcos.aiservice.quizz;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Profile("dev")
@RestController
@RequestMapping("/quizz/{userId}")
@RequiredArgsConstructor
public class QuizzReviewController {

    public record Summary(UUID id, UUID fileId, QuizzStatus status, Strategy strategy, Instant createdAt) {}
    public record QuestionReview(String text, List<String> options, int correctIndex, String explanation) {}
    public record Review(Summary summary, List<QuestionReview> questions) {}

    private final QuizzRepository repository;

    @GetMapping
    public List<Summary> list(@PathVariable UUID userId) {
        return repository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Quizz::getCreatedAt).reversed())
                .map(QuizzReviewController::summary)
                .toList();
    }

    @GetMapping("/{id}/review")
    @Transactional(readOnly = true)
    public Review review(@PathVariable UUID userId, @PathVariable UUID id) {
        var quizz = repository.findById(id)
                .filter(q -> q.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Quizz not found"));
        var questions = quizz.getQuestions().stream()
                .map(q -> new QuestionReview(q.getText(), q.getOptions(), q.getCorrectIndex(), q.getExplanation()))
                .toList();
        return new Review(summary(quizz), questions);
    }

    private static Summary summary(Quizz q) {
        return new Summary(q.getId(), q.getFileId(), q.getStatus(), q.getStrategy(), q.getCreatedAt());
    }
}
