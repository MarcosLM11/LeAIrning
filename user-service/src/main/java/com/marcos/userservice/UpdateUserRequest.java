package com.marcos.userservice;

import jakarta.validation.constraints.NotBlank;

record UpdateUserRequest(
        @NotBlank String name
) {}