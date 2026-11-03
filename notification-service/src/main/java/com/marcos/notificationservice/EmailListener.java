package com.marcos.notificationservice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
class EmailListener {

    static final String TOPIC = "notification.email";
    private final EmailService service;

    @KafkaListener(topics = TOPIC)
    void onEmailRequested(@Payload @Valid EmailRequestedEvent event) {
        service.send(event);
    }
}
