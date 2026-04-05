package com.moodflix.gateway.controller;

import com.moodflix.gateway.auth.model.RegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whenRegister_thenReceiveSuccessMessage() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@test.com");

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(val -> val.contains("User registered"));
    }
}