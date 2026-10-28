package com.moodflix.gateway.profile.controller;

import com.moodflix.gateway.auth.repository.UserRepository;
import com.moodflix.gateway.auth.service.JwtService;
import com.moodflix.gateway.profile.model.InteractionEvent;
import com.moodflix.gateway.profile.model.InteractionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Handles incoming user interaction requests (Like, Dislike, Skip).
 * <p>
 * Non-blocking ingestion: validates JWT, builds deterministic eventId,
 * publishes to Kafka with key=userId, and returns 202 Accepted in <5ms.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public final class ProfileController {

    private static final String TOPIC = "user-interactions-topic";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping("/interactions")
    public Mono<ResponseEntity<Void>> recordInteraction(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody InteractionRequest request
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token"));
        }

        String username;
        try {
            username = jwtService.validate(authHeader.substring(7)).username();
        } catch (Exception e) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        }

        String effectiveCorrelationId = (correlationId != null && !correlationId.isBlank())
                ? correlationId
                : UUID.randomUUID().toString();

        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .flatMap(user -> {
                    UUID userId = user.getId();
                    String eventId = generateDeterministicEventId(userId, request.movieId(), request.interactionType());

                    InteractionEvent event = new InteractionEvent(
                            eventId,
                            userId,
                            request.movieId(),
                            request.interactionType(),
                            request.weight(),
                            Instant.now(),
                            effectiveCorrelationId
                    );

                    // Publish to Kafka with partition key = userId (ensures per-user FIFO order)
                    kafkaTemplate.send(TOPIC, userId.toString(), event);
                    log.info("[GATEWAY INGEST] Sent eventId: {} for user: {} to Kafka", eventId, userId);

                    // Return 202 Accepted immediately
                    return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED).<Void>build());
                });
    }

    private String generateDeterministicEventId(UUID userId, String movieId, InteractionRequest.InteractionType type) {
        String raw = userId + ":" + movieId + ":" + type;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
        }
    }
}