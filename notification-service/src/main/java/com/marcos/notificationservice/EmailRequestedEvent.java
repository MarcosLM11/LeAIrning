package com.marcos.notificationservice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequestedEvent(
        @NotBlank @Email String to,
        @NotBlank String subject,
        @NotBlank String body
) {}
