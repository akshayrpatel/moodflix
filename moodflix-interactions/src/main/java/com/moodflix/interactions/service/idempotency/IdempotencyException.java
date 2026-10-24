package com.moodflix.interactions.service.idempotency;

/**
 * Thrown when the distributed idempotency store (Redis) fails or is unreachable.
 * Indicates an infrastructure failure that warrants a consumer retry.
 */
public class IdempotencyException extends RuntimeException {

    public IdempotencyException(String message) {
        super(message);
    }

    public IdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}