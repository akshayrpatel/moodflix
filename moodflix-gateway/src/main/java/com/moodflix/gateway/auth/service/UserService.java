package com.moodflix.gateway.auth.service;

import com.moodflix.gateway.auth.model.User;
import com.moodflix.gateway.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handles user registration and authentication for MoodFlix.
 *
 * <p>Delegates persistence to {@link UserRepository} and token
 * generation to {@link JwtService}. Password hashing is handled
 * by the injected {@link PasswordEncoder} — raw passwords are
 * never stored or logged.</p>
 */
@Slf4j
@Service
public final class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userRepository  the reactive repository for user persistence
     * @param jwtService      the JWT token generation contract
     * @param passwordEncoder the BCrypt password encoder
     */
    public UserService(
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.jwtService      = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user account with a hashed password.
     *
     * <p>UUID is generated explicitly before save — R2DBC does not
     * support {@code @GeneratedValue} with UUID strategy.</p>
     *
     * @param username the desired username
     * @param email    the user's email address
     * @param password the raw password — hashed before persistence
     * @return a {@link Mono} emitting the saved {@link User}
     */
    public Mono<User> registerUser(String username, String email, String password) {
        log.debug("Register attempt for user: {}", username);
        User user = User.builder()
                .id(UUID.randomUUID())      // R2DBC requires explicit ID generation
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .build();

        return userRepository.save(user);
    }

    /**
     * Authenticates a user and returns a signed JWT token.
     *
     * <p>Both invalid username and wrong password return the same
     * {@link BadCredentialsException} — intentional to prevent
     * username enumeration attacks.</p>
     *
     * @param username the username to authenticate
     * @param password the raw password to verify
     * @return a {@link Mono} emitting the signed JWT token
     */
    public Mono<String> login(String username, String password) {
        log.debug("Login attempt for user: {}", username);
        return userRepository.findByUsername(username)
                .flatMap(user -> {
                    if (passwordEncoder.matches(password, user.getPasswordHash())) {
                        return Mono.just(jwtService.generateToken(username));
                    }
                    return Mono.error(new BadCredentialsException("Bad Creds"));
                })
                .switchIfEmpty(Mono.error(new BadCredentialsException("Bad Creds"))); // this is intentional to keep user list private
    }
}