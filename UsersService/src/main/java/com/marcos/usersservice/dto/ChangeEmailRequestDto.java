package com.marcos.usersservice.dto;

import jakarta.validation.constraints.Email;

public record ChangeEmailRequestDto(
        @Email
        String newEmail
) {}
