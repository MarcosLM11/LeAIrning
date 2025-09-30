package com.marcos.usersservice.dto;

public record CreateUserRequestDto(
         String username,
         String password,
         String email,
         String firstName,
         String lastName,
         String phoneNumber
) {}
