package com.moodflix.gateway.risk.model;

import java.time.Instant;

/**
 * Immutable snapshot of an observed response in the gateway.
 *
 * <p>Created by {@code ThreatPatternFilter} after the filter chain
 * completes. Wraps all response data into a single typed object —
 * eliminates passing username, status, and endpoint as separate
 * parameters through private methods.</p>
 *
 * @param username    the authenticated user who made the request
 * @param statusCode  the HTTP status code of the response
 * @param endpoint    the endpoint that was accessed
 * @param observedAt  when this response was observed
 */
public record ResponseContext(
        String username,
        int statusCode,
        String endpoint,
        Instant observedAt
) {}