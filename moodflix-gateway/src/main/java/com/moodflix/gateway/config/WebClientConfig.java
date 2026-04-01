package com.moodflix.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures a shared {@link WebClient} bean for calling the
 * MoodFlix FastAPI AI engine.
 *
 * <p>A single shared instance is used across the application —
 * creating a new {@link WebClient} per request is expensive.
 * The base URL is read from {@code application.yml} so it can
 * be changed per environment without code changes.</p>
 *
 * <p>Uses Spring WebFlux's non-blocking HTTP client — never
 * blocks a thread waiting for FastAPI to respond.</p>
 */
@Configuration
public class WebClientConfig {

    @Value("${ai.engine.url}")
    private String aiEngineUrl;

    /**
     * Declares the shared WebClient bean for FastAPI communication.
     *
     * <p>All services that need to call FastAPI inject this bean
     * rather than creating their own instances.</p>
     *
     * @return a configured {@link WebClient} pointed at FastAPI
     */
    @Bean
    public WebClient aiEngineWebClient() {
        return WebClient.builder()
                .baseUrl(aiEngineUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}