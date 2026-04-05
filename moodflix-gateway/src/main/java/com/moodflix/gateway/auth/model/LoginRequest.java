package com.moodflix.gateway.auth.model;

/**
 * Immutable data carrier for user login requests.
 *
 * <p>Carries the two fields required to authenticate an existing user.
 * Deserialized from the JSON request body by Spring WebFlux.</p>
 *
 * @param username the username to authenticate
 * @param password the raw password to verify against the stored hash
 */
public record LoginRequest(
        String username,
        String password
) {}