package com.marcos.usersservice.dto;

public record UpdateUserRequestDto(
        String firstName,
        String lastName,
        String phoneNumber
) {}
