package com.marcos.aiservice.quizz;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static java.util.stream.Collectors.toMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class QuizzService {

    static final String TOPIC = "quizz.requested";
    private final QuizzRepository repository;
    private final AttemptRepository attempts;
    private final KafkaTemplate<String, QuizzRequestedEvent> kafka;

    public UUID start(UUID userId, String email, UUID fileId, int questions, int options, Difficulty difficulty) {
        if (questions < 1 || questions > 50 || options < 2 || options > 6) {
            throw new ResponseStatusException(BAD_REQUEST, "questions must be 1-50 and options 2-6");
        }
        var quizz = repository.save(new Quizz(fileId, userId, questions, options, difficulty));
        send(quizz, email);
        return quizz.getId();
    }

    // Not @Transactional on purpose: the status must be committed before the event, or the listener would skip it.
    public void retry(UUID userId, String email, UUID id) {
        var quizz = find(userId, id);
        if (quizz.getStatus() != QuizzStatus.FAILED) {
            throw new ResponseStatusException(CONFLICT, "Only failed quizzes can be retried");
        }
        quizz.setStatus(QuizzStatus.PENDING);
        repository.save(quizz);
        send(quizz, email);
    }

    private void send(Quizz q, String email) {
        kafka.send(TOPIC, q.getId().toString(), new QuizzRequestedEvent(q.getId(), q.getUserId(), email, q.getFileId(),
                q.getRequestedQuestions(), q.getOptions(), q.getDifficulty()));
    }

    public List<QuizzSummary> list(UUID userId) {
        var scores = attempts.findLatestScores(userId).stream()
                .collect(toMap(AttemptRepository.LatestScore::getQuizzId, AttemptRepository.LatestScore::getScore, (a, b) -> a));
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(q -> QuizzSummary.of(q, scores.get(q.getId())))
                .toList();
    }

    public QuizzView get(UUID userId, UUID id) {
        return QuizzView.of(find(userId, id));
    }

    /** Grades the answers on the server and stores the attempt; the solutions are only revealed here. */
    public AttemptResult submit(UUID userId, UUID id, List<Integer> answers) {
        var quizz = find(userId, id);
        if (quizz.getStatus() != QuizzStatus.READY) {
            throw new ResponseStatusException(CONFLICT, "Quizz is not ready");
        }
        var questions = quizz.getQuestions();
        if (answers == null || answers.size() != questions.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "one answer per question is required");
        }
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            var answer = answers.get(i);
            if (answer == null || answer < 0 || answer >= questions.get(i).options().size()) {
                throw new ResponseStatusException(BAD_REQUEST, "answer " + (i + 1) + " is out of range");
            }
            if (answer == questions.get(i).correctIndex()) {
                score++;
            }
        }
        attempts.save(new Attempt(id, userId, answers, score));
        var corrections = questions.stream()
                .map(q -> new AttemptResult.Correction(q.correctIndex(), q.explanation()))
                .toList();
        return new AttemptResult(score, questions.size(), corrections);
    }

    private Quizz find(UUID userId, UUID id) {
        return repository.findById(id)
                .filter(quizz -> quizz.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Quizz not found"));
    }

    @Transactional
    public void deleteByFile(UUID fileId) {
        var quizzes = repository.findByFileId(fileId);
        attempts.deleteByQuizzIdIn(quizzes.stream().map(Quizz::getId).toList());
        repository.deleteAll(quizzes);
    }

    @Transactional
    public void complete(UUID id, GeneratedQuizz generated) {
        repository.findById(id).filter(quizz -> quizz.getStatus() == QuizzStatus.PENDING).ifPresent(quizz -> {
            quizz.setQuestions(generated.questions());
            quizz.setStatus(QuizzStatus.READY);
        });
    }

    @Transactional
    public void fail(UUID id) {
        repository.findById(id).filter(quizz -> quizz.getStatus() == QuizzStatus.PENDING)
                .ifPresent(quizz -> quizz.setStatus(QuizzStatus.FAILED));
    }
}