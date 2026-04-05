package com.moodflix.gateway.auth.controller;

import com.moodflix.gateway.auth.model.LoginRequest;
import com.moodflix.gateway.auth.model.RegistrationRequest;
import com.moodflix.gateway.auth.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Handles user authentication and registration for the MoodFlix gateway.
 *
 * <p>Exposes two public endpoints under {@code /auth} that bypass
 * JWT validation in {@code JwtAuthFilter}. All other routes require
 * a valid JWT.</p>
 *
 * <p>Delegates all business logic to {@link UserService} — this
 * controller only handles HTTP concerns.</p>
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    /**
     * @param userService the service handling registration and login logic
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registers a new user account.
     *
     * <p>Returns the generated user ID on success. Returns 409 Conflict
     * if the username or email is already taken. Returns 500 for any
     * other unexpected error.</p>
     *
     * @param request the registration details
     * @return a {@link Mono} emitting the new user's ID as a string
     */
    @PostMapping("/register")
    public Mono<String> register(@RequestBody RegistrationRequest request) {
        log.info("[AUTH] Registration request | user={}", request.username());

        return userService.registerUser(
                        request.username(),
                        request.email(),
                        request.password()
                )
                .map(user -> "User registered with ID: " + user.getId())
                .onErrorResume(DataIntegrityViolationException.class, e -> {
                    log.warn("[AUTH] Duplicate registration | user={}", request.username());
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Username or email already exists"
                    ));
                });
    }

    /**
     * Authenticates an existing user and returns a JWT token.
     *
     * <p>Returns 401 Unauthorized if credentials are invalid.</p>
     *
     * @param request the login credentials
     * @return a {@link Mono} emitting the signed JWT token
     */
    @PostMapping("/login")
    public Mono<String> login(@RequestBody LoginRequest request) {
        log.info("[AUTH] Login request | user={}", request.username());

        return userService.login(request.username(), request.password())
                .onErrorResume(RuntimeException.class, e -> {
                    log.warn("[AUTH] Login failed | user={} reason={}",
                            request.username(), e.getMessage());
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid credentials"
                    ));
                });
    }
}