package com.moodflix.gateway.common;

/**
 * Central registry of all HTTP header names used across the
 * MoodFlix gateway filter chain.
 *
 * <p>Defining header names as constants in one place ensures
 * consistency across all filters — a header renamed here is
 * automatically updated everywhere it is referenced. No magic
 * strings scattered across the codebase.</p>
 *
 * <p>All constants are intentionally {@code public static final}
 * so they can be referenced directly without instantiation.</p>
 */
public final class GatewayHeaders {

    private GatewayHeaders() {
        // utility class — no instantiation
    }

    // ── Identity Headers ────────────────────────────────────────

    /** Injected by {@code JwtAuthFilter} after JWT validation. */
    public static final String USERNAME         = "X-Username";

    /** Injected by {@code InboundHeaderSanitizationFilter} — "guest" or "authenticated". */
    public static final String USER_TYPE        = "X-User-Type";
    public static final String USER_TYPE_GUEST         = "guest";
    public static final String USER_TYPE_AUTHENTICATED = "authenticated";

    /** Standard HTTP authorization header — stripped after validation. */
    public static final String AUTHORIZATION    = "Authorization";


    // ── Risk Headers ─────────────────────────────────────────────

    /** Injected by {@code ContextualRiskFilter} — LOW, MEDIUM, or HIGH. */
    public static final String RISK_LEVEL       = "X-Risk-Level";

    /** Injected by {@code ContextualRiskFilter} — numeric risk score. */
    public static final String RISK_SCORE       = "X-Risk-Score";

    /** Injected by {@code ContextualRiskFilter} — ALLOW, CHALLENGE, or BLOCK. */
    public static final String RISK_ACTION      = "X-Risk-Action";


    // ── Routing Headers ──────────────────────────────────────────

    /** Injected by {@code VectorHeaderFilter} — user taste vector. */
    public static final String USER_VECTOR      = "X-User-Vector";

    /** Injected by {@code CorrelationIdFilter} — request trace ID. */
    public static final String CORRELATION_ID   = "X-Correlation-ID";


    // ── Proxy Headers ────────────────────────────────────────────

    /** Standard proxy header — real client IP behind load balancer. */
    public static final String FORWARDED_FOR    = "X-Forwarded-For";

    /** Standard proxy header — real client IP. */
    public static final String REAL_IP          = "X-Real-IP";
}