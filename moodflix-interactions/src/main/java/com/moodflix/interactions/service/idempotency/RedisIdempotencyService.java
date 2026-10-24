package com.moodflix.interactions.service.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed implementation of {@link IdempotencyService} using atomic SETNX primitives.
 * <p>
 * Enforces fail-fast semantics: if Redis is unreachable, throws {@link IdempotencyException}
 * to trigger Kafka listener container retries rather than silently processing duplicates.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:event:";
    private static final Duration CLAIM_TTL = Duration.ofSeconds(30);
    private static final Duration PERSIST_TTL = Duration.ofHours(24);

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_PROCESSED = "PROCESSED";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean acquireLock(String eventId) {
        String key = IDEMPOTENCY_KEY_PREFIX + eventId;
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, STATUS_IN_PROGRESS, CLAIM_TTL);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Acquired idempotency lease for eventId: {}", eventId);
                return true;
            }

            log.warn("Duplicate or in-flight event detected. Skipping eventId: {}", eventId);
            return false;
        } catch (DataAccessException ex) {
            log.error("Failed to connect to Redis during acquireLock for eventId: {}", eventId, ex);
            throw new IdempotencyException("Idempotency store unavailable during acquireLock", ex);
        }
    }

    @Override
    public void persistLock(String eventId) {
        String key = IDEMPOTENCY_KEY_PREFIX + eventId;
        try {
            redisTemplate.opsForValue().set(key, STATUS_PROCESSED, PERSIST_TTL);
            log.debug("Persisted processed lock for eventId: {}", eventId);
        } catch (DataAccessException ex) {
            log.error("Failed to persist processed status in Redis for eventId: {}", eventId, ex);
            throw new IdempotencyException("Idempotency store unavailable during persistLock", ex);
        }
    }

    @Override
    public void releaseLock(String eventId) {
        String key = IDEMPOTENCY_KEY_PREFIX + eventId;
        try {
            redisTemplate.delete(key);
            log.debug("Released lock for eventId: {} to allow retry", eventId);
        } catch (DataAccessException ex) {
            log.error("Failed to release lock in Redis for eventId: {}", eventId, ex);
            // We log but do not rethrow on release to avoid masking original business exceptions
        }
    }
}