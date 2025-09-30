package com.marcos.usersservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequestDto(
        @NotBlank(message = "New username is mandatory")
        @Size(min = 3, max = 50, message = "New username must be between 3 and 50 characters")
        String newUsername
) {}
