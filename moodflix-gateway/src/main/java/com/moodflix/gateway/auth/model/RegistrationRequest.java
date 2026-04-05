package com.moodflix.gateway.auth.model;

/**
 * Immutable data carrier for user registration requests.
 *
 * <p>Carries the three fields required to create a new user account.
 * Deserialized from the JSON request body by Spring WebFlux.</p>
 *
 * @param username the desired username
 * @param email    the user's email address
 * @param password the raw password — hashed before persistence
 */
public record RegistrationRequest(
        String username,
        String email,
        String password
) {}
