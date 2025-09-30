package com.marcos.usersservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
        @NotBlank(message = "Old password is mandatory")
        @Size(min = 6, max = 100, message = "Old password must be between 6 and 100 characters")
        String oldPassword,
        @NotBlank(message = "New password is mandatory")
        @Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
        String newPassword
) {}
