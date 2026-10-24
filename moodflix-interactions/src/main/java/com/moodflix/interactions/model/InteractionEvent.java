package com.moodflix.interactions.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event representing a user interaction (like, dislike, skip) on a media title.
 * <p>
 * This event acts as the canonical data contract between the {@code moodflix-gateway} (producer)
 * and the {@code moodflix-interactions} vector worker (consumer) via Kafka topic {@code user-interactions-topic}.
 * </p>
 *
 * <h2>Distributed Systems Guarantees:</h2>
 * <ul>
 *   <li><b>Partition Key:</b> {@link #userId()} is used as the Kafka message key to guarantee
 *       FIFO chronological ordering for a specific user across consumer rebalances.</li>
 *   <li><b>Idempotency Key:</b> {@link #eventId()} is a deterministic SHA-256 hash derived from
 *       {@code userId:movieId:interactionType} used with Redis SETNX to discard network retries.</li>
 *   <li><b>Distributed Tracing:</b> {@link #correlationId()} is propagated from Gateway HTTP headers
 *       to correlate logs across microservice boundaries.</li>
 * </ul>
 *
 * @param eventId         unique deterministic hash identifying this specific action
 * @param userId          UUID of the user performing the interaction
 * @param movieId         unique identifier of the movie or TV show
 * @param interactionType type of interaction (e.g. LIKE, DISLIKE)
 * @param weight          mathematical shift weight applied to vector algebra (e.g. +0.1, -0.2)
 * @param timestamp       time at which the interaction was captured at the Gateway
 * @param correlationId   trace identifier for distributed logging
 */
public record InteractionEvent(
        String eventId,
        UUID userId,
        String movieId,
        InteractionType interactionType,
        double weight,
        Instant timestamp,
        String correlationId
) {
    /**
     * Supported user interaction categories mapped to recommendation shifts.
     */
    public enum InteractionType {
        LIKE,
        DISLIKE,
        WATCHLIST,
        WATCHED,
        SKIP,
        REWATCH
    }
}