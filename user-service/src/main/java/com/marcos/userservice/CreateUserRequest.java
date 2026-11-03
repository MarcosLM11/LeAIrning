package com.marcos.userservice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String name
) {}