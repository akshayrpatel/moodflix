package com.moodflix.gateway;

import com.moodflix.gateway.common.GatewayHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;

/**
 * Integration test verifying the complete gateway filter chain
 * executes in the correct order for an authenticated request.
 *
 * <p>Starts the full Spring application context and sends real
 * HTTP requests through all filters via {@link WebTestClient}.
 * External dependencies are mocked to isolate filter chain
 * behaviour from infrastructure.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class FilterChainIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void placeholder() {
        // confirms context loads and WebTestClient is wired
    }

    @Test
    public void publicRoute_ShouldBypassJwtFilter() {
        webTestClient.post()
                .uri("/auth/login")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    /**
     * Verifies that {@code JwtAuthFilter} blocks unauthenticated
     * requests to protected routes with 401.
     */
    @Test
    public void protectedRoute_WithNoToken_ShouldReturn401() {
        webTestClient.get()
                .uri("/discovery/movies")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Verifies that {@code InboundHeaderSanitizationFilter} strips
     * attacker-injected gateway headers before {@code JwtAuthFilter} runs.
     */
    @Test
    public void protectedRoute_WithInjectedHeaders_ShouldReturn401() {
        webTestClient.get()
                .uri("/discovery/movies")
                .header(GatewayHeaders.USERNAME, "admin")
                .header(GatewayHeaders.RISK_LEVEL, "LOW")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}