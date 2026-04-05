package com.moodflix.gateway.auth.service;

import com.moodflix.gateway.auth.model.AuthenticatedPrincipal;

/**
 * Contract for JWT token operations in the MoodFlix gateway.
 *
 * <p>Abstracting JWT operations behind this interface decouples the
 * filter chain from any specific JWT library.</p>
 *
 * <p>All implementations must be stateless and thread-safe —
 * they are injected as singleton Spring beans.</p>
 */
public interface JwtService {

    /**
     * Generates a signed JWT token for the given username.
     *
     * @param username the authenticated user to issue a token for
     * @return a signed JWT string
     */
    String generateToken(String username);

    /**
     * Validates a JWT token and extracts the verified identity.
     *
     * <p>Returns an {@link AuthenticatedPrincipal} containing the
     * username, token issue time, and roles extracted from claims.
     * Throws a specific exception if the token is expired, tampered,
     * or otherwise invalid — never returns null.</p>
     *
     * @param token the raw JWT string to validate
     * @return the verified {@link AuthenticatedPrincipal}
     * @throws io.jsonwebtoken.ExpiredJwtException    if the token has expired
     * @throws io.jsonwebtoken.JwtException           if the token is malformed or tampered
     * @throws IllegalArgumentException               if the token string is blank or null
     */
    AuthenticatedPrincipal validate(String token);
}