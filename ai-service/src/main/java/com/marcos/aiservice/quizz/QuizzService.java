package com.marcos.aiservice.quizz;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
    private final KafkaTemplate<String, QuizzRequestedEvent> kafka;

    public UUID start(UUID userId, String email, UUID fileId, int questions, int options, Difficulty difficulty) {
        if (questions < 1 || questions > 50 || options < 2 || options > 6) {
            throw new ResponseStatusException(BAD_REQUEST, "questions must be 1-50 and options 2-6");
        }
        var id = repository.save(new Quizz(fileId, userId)).getId();
        kafka.send(TOPIC, id.toString(), new QuizzRequestedEvent(id, userId, email, fileId, questions, options, difficulty));
        return id;
    }

    public QuizzView get(UUID userId, UUID id) {
        return repository.findById(id)
                .filter(quizz -> quizz.getUserId().equals(userId))
                .map(QuizzView::of)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Quizz not found"));
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