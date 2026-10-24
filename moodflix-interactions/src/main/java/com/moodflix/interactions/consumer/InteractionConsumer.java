package com.moodflix.interactions.consumer;

import com.moodflix.interactions.model.InteractionEvent;
import com.moodflix.interactions.service.vector.TasteVectorUpdateService;
import com.moodflix.interactions.service.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Inbound Kafka consumer that processes user interaction events.
 * <p>
 * Enforces the Two-Phase Idempotency Gate (acquire -> process -> persist)
 * to ensure exactly-once processing semantics over an at-least-once transport.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionConsumer {

    private final TasteVectorUpdateService tasteVectorUpdateService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = "user-interactions-topic",
            groupId = "moodflix-interactions-group"
    )
    public void consumeInteraction(
            @Payload InteractionEvent event,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String partitionKey,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        String eventId = event.eventId();

        log.info("[KAFKA CONSUME] Received eventId: {} | user: {} | movie: {} | partition: {} | offset: {}",
                eventId, event.userId(), event.movieId(), partition, offset);

        // 1. Idempotency Gate: Pre-claim the event lease (30s TTL)
        if (!idempotencyService.acquireLock(eventId)) {
            log.warn("[IDEMPOTENCY] Dropping duplicate or in-flight eventId: {} for user: {}", eventId, event.userId());
            return; // Acknowledged and skipped with zero DB mutations
        }

        try {
            // 2. Business Process: Fetch, Calculate Shift, Save to Postgres, Evict Cache
            tasteVectorUpdateService.processInteraction(event);

            // 3. Confirm: Seal the lock for 24 hours
            idempotencyService.persistLock(eventId);
            log.info("[KAFKA SUCCESS] Successfully completed interaction for eventId: {}", eventId);

        } catch (Exception ex) {
            // 4. Compensating Action: Release the lease so Kafka retries can re-attempt
            idempotencyService.releaseLock(eventId);
            log.error("[KAFKA ERROR] Failed processing eventId: {}. Released lock to enable retry.", eventId, ex);

            // Re-throw to trigger Spring Kafka's error handler (backoff / DLQ)
            throw ex;
        }
    }
}