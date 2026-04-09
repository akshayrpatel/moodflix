package com.moodflix.gateway.risk.model;

import java.time.Instant;

/**
 * Immutable record of an incoming request persisted in a context.
 *
 * <p>Created once per request in {@code ContextualRiskFilter} and
 * never modified — safe to share across the reactive pipeline.</p>
 *
 * @param username      the verified username from {@code X-Username} header
 * @param ipAddress     the real client IP — X-Forwarded-For takes precedence
 *                      over remote address to handle load balancer scenarios
 * @param userAgent     the client User-Agent string
 * @param requestTime   when this request arrived at the gateway
 */
public record RequestContext(
        String username,
        String ipAddress,
        String userAgent,
        Instant requestTime
) {}