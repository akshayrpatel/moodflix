package com.moodflix.gateway.service;

import com.moodflix.gateway.auth.service.JwtService;
import com.moodflix.gateway.auth.model.User;
import com.moodflix.gateway.auth.service.UserService;
import com.moodflix.gateway.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 *
 * <p>Verifies registration and login behaviour including success paths,
 * wrong credentials, non-existent users, and duplicate registration.
 * All repository interactions are mocked to return reactive types.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // ========================= LOGIN TESTS =========================

    @Test
    void login_WithCorrectPassword_ShouldReturnToken() {
        User mockUser = User.builder()
                .id(UUID.randomUUID())
                .username("akshay")
                .passwordHash("hashed_pwd")
                .build();

        // findByUsername now returns Mono<User>
        when(userRepository.findByUsername("akshay")).thenReturn(Mono.just(mockUser));
        when(passwordEncoder.matches("correct_pwd", "hashed_pwd")).thenReturn(true);
        when(jwtService.generateToken("akshay")).thenReturn("mock.jwt.token");

        StepVerifier.create(userService.login("akshay", "correct_pwd"))
                .assertNext(token -> assertThat(token).isEqualTo("mock.jwt.token"))
                .verifyComplete();

        verify(jwtService).generateToken("akshay");
    }

    @Test
    void login_WithWrongPassword_ShouldReturnError() {
        User mockUser = User.builder()
                .id(UUID.randomUUID())
                .username("akshay")
                .passwordHash("hashed_pwd")
                .build();

        when(userRepository.findByUsername("akshay")).thenReturn(Mono.just(mockUser));
        when(passwordEncoder.matches("wrong_pwd", "hashed_pwd")).thenReturn(false);

        StepVerifier.create(userService.login("akshay", "wrong_pwd"))
                .expectErrorMatches(ex -> ex.getMessage().equals("Bad Creds"))
                .verify();

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_WithNonExistentUser_ShouldReturnError() {
        // Empty Mono — user not found, switchIfEmpty triggers Bad Creds
        when(userRepository.findByUsername("ghost")).thenReturn(Mono.empty());

        StepVerifier.create(userService.login("ghost", "any_pwd"))
                .expectErrorMatches(ex -> ex.getMessage().equals("Bad Creds"))
                .verify();

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    // ====================== REGISTER TESTS =========================

    @Test
    void register_WithValidData_ShouldReturnSavedUser() {
        UUID generatedId = UUID.randomUUID();

        User savedUser = User.builder()
                .id(generatedId)
                .username("akshay")
                .email("akshay@moodflix.com")
                .passwordHash("bcrypt_hash")
                .build();

        when(passwordEncoder.encode("raw_password")).thenReturn("bcrypt_hash");

        // save() now returns Mono<User> in R2DBC
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));

        StepVerifier.create(userService.registerUser("akshay", "akshay@moodflix.com", "raw_password"))
                .assertNext(user -> {
                    assertThat(user.getUsername()).isEqualTo("akshay");
                    assertThat(user.getEmail()).isEqualTo("akshay@moodflix.com");
                    assertThat(user.getPasswordHash()).isEqualTo("bcrypt_hash");
                    assertThat(user.getId()).isNotNull();
                })
                .verifyComplete();

        verify(passwordEncoder).encode("raw_password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WithDuplicateUsername_ShouldPropagateError() {
        when(passwordEncoder.encode("raw_password")).thenReturn("bcrypt_hash");

        // R2DBC propagates DB errors as Mono.error() — not thrown exceptions
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("Unique constraint violated")));

        StepVerifier.create(userService.registerUser("akshay", "akshay@moodflix.com", "raw_password"))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }
}