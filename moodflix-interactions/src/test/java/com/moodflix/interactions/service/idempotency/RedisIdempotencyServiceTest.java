package com.moodflix.interactions.service.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RedisIdempotencyService} using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class RedisIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        // Wire the mock ValueOperations into StringRedisTemplate
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new RedisIdempotencyService(redisTemplate);
    }

    @Test
    @DisplayName("acquireLock should return true when key is absent in Redis (first time)")
    void acquireLock_returnsTrueWhenFirstSeen() {
        when(valueOperations.setIfAbsent(
                eq("idempotency:event:evt-101"),
                eq("IN_PROGRESS"),
                eq(Duration.ofSeconds(30))
        )).thenReturn(true);

        boolean acquired = idempotencyService.acquireLock("evt-101");

        assertTrue(acquired, "Must grant lock when event is new");
    }

    @Test
    @DisplayName("acquireLock should return false when key already exists in Redis (duplicate)")
    void acquireLock_returnsFalseWhenDuplicate() {
        when(valueOperations.setIfAbsent(
                eq("idempotency:event:evt-101"),
                eq("IN_PROGRESS"),
                eq(Duration.ofSeconds(30))
        )).thenReturn(false);

        boolean acquired = idempotencyService.acquireLock("evt-101");

        assertFalse(acquired, "Must reject lock when event is duplicate");
    }

    @Test
    @DisplayName("persistLock should promote key to PROCESSED with 24h TTL")
    void persistLock_updatesKeyWithConfirmedStatus() {
        idempotencyService.persistLock("evt-101");

        verify(valueOperations, times(1)).set(
                eq("idempotency:event:evt-101"),
                eq("PROCESSED"),
                eq(Duration.ofHours(24))
        );
    }

    @Test
    @DisplayName("releaseLock should delete key from Redis to allow retries")
    void releaseLock_deletesKey() {
        idempotencyService.releaseLock("evt-101");

        verify(redisTemplate, times(1)).delete("idempotency:event:evt-101");
    }
}