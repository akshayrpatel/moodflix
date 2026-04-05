package com.moodflix.gateway.auth.service;

import com.moodflix.gateway.auth.model.AuthenticatedPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JJWT-based implementation of {@link JwtService}.
 *
 * <p>Handles token generation and validation using HMAC-SHA256
 * signing. The signing key is built once at construction time
 * from the configured secret — never rebuilt per request.</p>
 *
 */
@Slf4j
@Service
public final class JwtServiceImpl implements JwtService {

    private static final long EXPIRY_DURATION_MS = 1000L * 60 * 60 * 24; // 24 hours

    private final SecretKey signingKey;

    /**
     * Builds the signing key once at startup from the configured secret.
     *
     * @param secret the JWT secret from {@code application.yml}
     */
    public JwtServiceImpl(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Issues a token valid for 24 hours from the current time.
     * Roles are not embedded in the token until role-based access
     * control is fully implemented.</p>
     */
    @Override
    public String generateToken(String username) {
        var now = Instant.now();

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(EXPIRY_DURATION_MS)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Parses and validates the token in a single operation.
     * Extracts subject and issued-at claims to build the
     * {@link AuthenticatedPrincipal}.</p>
     */
    @Override
    public AuthenticatedPrincipal validate(String token) {
        var claims = parseClaims(token);
        var username = claims.getSubject();
        var issuedAt = claims.getIssuedAt().toInstant();

        return new AuthenticatedPrincipal(username, issuedAt, List.of());
    }

    /**
     * Parses JWT claims from the token string.
     *
     * @param token the raw JWT string
     * @return the parsed {@link Claims}
     * @throws ExpiredJwtException  if the token has expired
     * @throws JwtException         if the token is malformed or tampered
     * @throws IllegalArgumentException if the token is blank or null
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}