package com.letterfuture.execution.engine.auth;


import com.letterfuture.execution.engine.config.JwtProperties;
import com.letterfuture.execution.engine.workflow.domain.Users;
import com.letterfuture.execution.engine.workflow.dto.MeResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(Users user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);

        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Users user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public UUID validateRefreshToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return UUID.fromString(claims.getSubject());
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid refresh token", e);
        }
    }

    public MeResponse getUserInfoFromAccessToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            return new MeResponse(userId, username, email, role);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid access token", e);
        }
    }
}
