package com.marcos.aiservice.quizz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class QuizzListener {

    private final QuizzGenerator generator;
    private final QuizzService service;
    private final KafkaTemplate<String, EmailRequestedEvent> kafka;

    static final String EMAIL_TOPIC = "notification.email";

    @KafkaListener(topics = QuizzService.TOPIC)
    void onQuizzRequested(QuizzRequestedEvent event) {
        try {
            service.complete(event.quizzId(), generator.generate(event.userId(), event.fileId(), event.questions(), event.options(), event.difficulty()));
            if (event.email() != null) {
                kafka.send(EMAIL_TOPIC, event.quizzId().toString(), new EmailRequestedEvent(event.email(), "Tu quizz está listo", "Tu quizz " + event.quizzId() + " ya se ha generado."));
            }
        } catch (Exception e) {
            log.error("Quizz {} failed", event.quizzId(), e);
            service.fail(event.quizzId());
        }
    }
}
