package com.marcos.usersservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequestDto(
        @NotBlank(message = "New email is mandatory")
        @Email(message = "New email must be a valid email address")
        String newEmail
) {}
