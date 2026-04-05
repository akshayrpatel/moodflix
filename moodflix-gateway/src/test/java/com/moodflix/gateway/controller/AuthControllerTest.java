package com.moodflix.gateway.controller;

import com.moodflix.gateway.auth.controller.AuthController;
import com.moodflix.gateway.auth.model.RegistrationRequest;
import com.moodflix.gateway.auth.model.User;
import com.moodflix.gateway.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(authController).build();
    }

    // ========================= REGISTER TESTS =========================

    @Test
    void register_WithValidData_ShouldReturnSuccessMessage() {
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setUsername("Ultron");

        when(userService.registerUser("Ultron", "ultron@avengers.com", "password123"))
                .thenReturn(Mono.just(savedUser));

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("Ultron");
        request.setEmail("ultron@avengers.com");
        request.setPassword("password123");

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("User registered with ID: " + savedUser.getId());
    }

    @Test
    void register_WithDuplicateUser_ShouldReturnErrorMessage() {
        when(userService.registerUser("Ultron", "ultron@avengers.com", "password123"))
                .thenReturn(Mono.error(new RuntimeException("Duplicate key")));

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("Ultron");
        request.setEmail("ultron@avengers.com");
        request.setPassword("password123");

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Error: Username or Email already exists!");
    }

    // ========================= LOGIN TESTS =========================

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        when(userService.login("Ultron", "password123"))
                .thenReturn(Mono.just("mock.jwt.token"));

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("Ultron");
        request.setPassword("password123");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("mock.jwt.token");
    }

    @Test
    void login_WithBadCredentials_ShouldReturn500() {
        when(userService.login("Ultron", "wrong_pwd"))
                .thenReturn(Mono.error(new RuntimeException("Bad Creds")));

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("Ultron");
        request.setPassword("wrong_pwd");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
