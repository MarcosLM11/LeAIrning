package com.marcos.userservice.security;

record Tokens(
        String accessToken,
        String refreshToken
) {}
