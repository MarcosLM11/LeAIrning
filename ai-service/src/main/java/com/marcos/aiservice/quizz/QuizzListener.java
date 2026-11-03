package com.marcos.aiservice.quizz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class QuizzListener {

    private final QuizzGenerator generator;
    private final QuizzService service;

    @KafkaListener(topics = QuizzService.TOPIC)
    void onQuizzRequested(QuizzRequestedEvent event) {
        try {
            service.complete(event.quizzId(), generator.generate(event.fileId(), event.questions(), event.options(), event.difficulty()));
        } catch (Exception e) {
            log.error("Quizz {} failed", event.quizzId(), e);
            service.fail(event.quizzId());
        }
    }
}
