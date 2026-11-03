package com.marcos.userservice.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import com.marcos.userservice.user.User;
import com.marcos.userservice.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
class AuthService {

    static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Duration TTL = Duration.ofHours(1);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    Tokens login(LoginRequest r) {
        var user = repository.findByUsernameOrEmail(r.identifier(), r.identifier())
                .filter(u -> passwordEncoder.matches(r.password(), u.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return issue(user);
    }

    @Transactional
    Tokens refresh(String rawToken) {
        var stored = refreshTokens.findByTokenHash(hash(rawToken))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        refreshTokens.delete(stored);

        var user = repository.findById(stored.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        return issue(user);
    }

    @Transactional
    void logout(String rawToken) {
        refreshTokens.deleteByTokenHash(hash(rawToken));
    }

    private Tokens issue(User user) {
        var now = Instant.now();

        var claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(TTL))
                .claim("roles", List.of(user.getRole().name()))
                .claim("email", user.getEmail())
                .build();

        var access = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        var refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(hash(refresh), user.getId(), now.plus(REFRESH_TTL)));

        return new Tokens(access, refresh);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}