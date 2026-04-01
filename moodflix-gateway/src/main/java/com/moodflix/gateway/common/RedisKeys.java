package com.moodflix.gateway.common;

import java.time.Duration;

/**
 * Central registry of all Redis key patterns and TTLs used
 * across the MoodFlix gateway.
 *
 * <p>Key naming convention: {@code domain:type:{variable}}</p>
 */
public final class RedisKeys {

    private RedisKeys() {}

    // ── Session Domain ───────────────────────────────────────────
    /**
     * Stores {@code SessionContext} for risk evaluation.
     * Used by {@code RedisSessionCache}.
     */
    public static final String SESSION_CONTEXT     = "session:ctx:";
    public static final Duration SESSION_TTL       = Duration.ofHours(24);


    // ── Vector Domain ────────────────────────────────────────────
    /**
     * Caches user taste vector fetched from PostgreSQL.
     * Used by {@code VectorCacheService}.
     */
    public static final String VECTOR_CACHE        = "vector:taste:";
    public static final Duration VECTOR_TTL        = Duration.ofHours(1);


    // ── Rate Limiting Domain ─────────────────────────────────────
    /**
     * Tracks request count per user per minute.
     * Used by {@code AdaptiveRateLimitFilter}.
     */
    public static final String RATE_LIMIT          = "rate:limit:";
    public static final Duration RATE_LIMIT_TTL    = Duration.ofMinutes(1);


    // ── Threat Detection Domain ──────────────────────────────────
    /**
     * Tracks repeated 401 auth failures per user.
     * Used by {@code ThreatPatternFilter}.
     */
    public static final String THREAT_401          = "threat:401:";
    public static final Duration THREAT_401_TTL    = Duration.ofSeconds(30);

    /**
     * Tracks repeated 403 forbidden responses per user.
     * Used by {@code ThreatPatternFilter}.
     */
    public static final String THREAT_403          = "threat:403:";
    public static final Duration THREAT_403_TTL    = Duration.ofSeconds(30);

    /**
     * Tracks burst request patterns per user.
     * Used by {@code ThreatPatternFilter}.
     */
    public static final String THREAT_BURST        = "threat:burst:";
    public static final Duration THREAT_BURST_TTL  = Duration.ofSeconds(10);
}