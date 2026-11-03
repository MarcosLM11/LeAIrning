package com.marcos.aiservice.quizz;

record EmailRequestedEvent(
        String to,
        String subject,
        String body
) {}
