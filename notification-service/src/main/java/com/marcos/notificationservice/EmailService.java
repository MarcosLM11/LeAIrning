package com.marcos.notificationservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationRepository repository;

    void send(EmailRequestedEvent event) {
        var status = NotificationStatus.SENT;
        try {
            var message = new SimpleMailMessage();
            message.setTo(event.to());
            message.setSubject(event.subject());
            message.setText(event.body());
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Email to {} failed", event.to(), e);
            status = NotificationStatus.FAILED;
        }
        repository.save(new Notification(event, status));
    }
}
