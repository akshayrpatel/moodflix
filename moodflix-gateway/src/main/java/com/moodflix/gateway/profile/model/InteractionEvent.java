package com.moodflix.gateway.profile.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Canonical event published to Kafka user-interactions-topic.
 */
public record InteractionEvent(
        String eventId,
        UUID userId,
        String movieId,
        InteractionRequest.InteractionType interactionType,
        double weight,
        Instant timestamp,
        String correlationId
) {}