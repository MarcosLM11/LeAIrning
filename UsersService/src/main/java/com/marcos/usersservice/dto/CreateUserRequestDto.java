package com.marcos.usersservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequestDto(
        @NotBlank(message = "Username is mandatory")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        @NotBlank(message = "Password is mandatory")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,
        @NotBlank(message = "Email is mandatory")
        @Email
        String email,
        @NotBlank(message = "First name is mandatory")
        @Size(min = 3, max = 20, message = "First name must be between 3 and 20 characters")
        String firstName,
        @Size(min = 3, max = 20, message = "Last name must be between 3 and 20 characters")
        String lastName,
        @Size(min = 6, max = 15, message = "Phone number must be between 6 and 15 characters")
        String phoneNumber
) {}
