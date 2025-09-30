package com.marcos.usersservice.dto;

public record ChangePasswordRequestDto(
        String oldPassword,
        String newPassword
) {}
