package com.marcos.userservice.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank String username,
        @NotBlank @Size(min = 8, max = 72) String password
) {}