package com.moodflix.interactions.service.idempotency;

/**
 * Service contract for managing distributed event idempotency.
 * <p>
 * Implements a Two-Phase "Claim-Mutate-Confirm" locking pattern to ensure that
 * duplicate Kafka messages (caused by client retries or consumer group rebalances)
 * do not cause cumulative vector drift or redundant database writes.
 * </p>
 *
 * <h2>Lifecycle:</h2>
 * <ol>
 *   <li>{@link #acquireLock(String)}: Pre-claims the event with a short lease (e.g. 30s) prior to computation.</li>
 *   <li>{@link #persistLock(String)}: Transitions event to permanently processed with a 24-hour TTL after successful DB persist.</li>
 *   <li>{@link #releaseLock(String)}: Compensating transaction that deletes the claim if calculation or DB persist throws an exception.</li>
 * </ol>
 */
public interface IdempotencyService {

    /**
     * Attempts to acquire an exclusive processing lease on an incoming event.
     *
     * @param eventId deterministic unique identifier for the interaction
     * @return {@code true} if lease is granted (first time event is seen);
     *         {@code false} if event is currently in-flight or already processed
     */
    boolean acquireLock(String eventId);

    /**
     * Confirms successful processing, extending the lock TTL to 24 hours.
     * Any subsequent delivery of this eventId within 24 hours will be discarded.
     *
     * @param eventId unique identifier for the interaction
     */
    void persistLock(String eventId);

    /**
     * Releases the processing lease.
     * Called in error-handling blocks to ensure transient failures can be retried by Kafka.
     *
     * @param eventId unique identifier for the interaction
     */
    void releaseLock(String eventId);
}