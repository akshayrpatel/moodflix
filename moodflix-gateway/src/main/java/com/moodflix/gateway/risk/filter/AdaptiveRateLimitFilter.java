package com.moodflix.gateway.risk.filter;

import com.moodflix.gateway.common.GatewayHeaders;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Fifth filter in the gateway chain (order = 2).
 *
 * <p>Enforces dynamic request rate limits based on the user's current
 * risk level. Unlike static rate limiters that treat all users equally,
 * this filter tightens limits as risk increases — a user flagged as
 * HIGH risk gets significantly fewer requests per minute than a LOW
 * risk user.</p>
 *
 * <p>Rate limits per minute:</p>
 * <ul>
 *   <li>{@code LOW}    — {@value LOW_LIMIT} requests per minute</li>
 *   <li>{@code MEDIUM} — {@value MEDIUM_LIMIT} requests per minute</li>
 *   <li>{@code HIGH}   — {@value HIGH_LIMIT} requests per minute</li>
 * </ul>
 *
 * <p>Reads {@code X-Risk-Level} injected by {@code ContextualRiskFilter}.
 * If the header is absent the filter defaults to LOW limits — safe
 * for public routes that bypass risk evaluation.</p>
 *
 * <p>On Redis failure the filter allows the request through — failing
 * open is intentional. Infrastructure failures must not block
 * legitimate users.</p>
 *
 * <p>Filter execution order:</p>
 * <pre>
 *   InboundHeaderSanitizationFilter (-4) → CorrelationIdFilter (-3) → JwtAuthFilter (-2)
 *   → ContextualRiskFilter (-1) → VectorHeaderFilter (0) → ThreatPatternFilter (1) → AdaptiveRateLimitFilter (2)
 * </pre>
 */
@Slf4j
@Component
public final class AdaptiveRateLimitFilter implements GlobalFilter, Ordered {

    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private static final int LOW_LIMIT = 100;
    private static final int MEDIUM_LIMIT = 50;
    private static final int HIGH_LIMIT = 10;

    private static final String KEY_PREFIX = "rate:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public AdaptiveRateLimitFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int getOrder() {
        return 2;
    }

    /**
     * Reads the user's risk level and enforces the corresponding
     * request limit before passing to the next filter.
     *
     * <p>Public routes without {@code X-Username} bypass this filter
     * entirely — rate limiting only applies to authenticated requests.</p>
     *
     * @param exchange the current server exchange
     * @param chain    the remaining filter chain
     * @return a {@link Mono} that completes when processing finishes
     */
    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        // X-Username was injected by JwtAuthFilter — we trust it
        String username = exchange.getRequest()
                .getHeaders()
                .getFirst(GatewayHeaders.USERNAME);

        if (username == null || username.isBlank()) {
            return chain.filter(exchange);
        }

        String redisKey = KEY_PREFIX + username;
        String riskLevel = exchange.getRequest()
                .getHeaders()
                .getFirst(GatewayHeaders.RISK_LEVEL);
        int limit = getRateLimit(riskLevel);
        return checkAndTrack(exchange, chain, redisKey, limit, username);
    }

    /**
     * Increments the request counter and either rejects with 429
     * or allows the request through.
     *
     * <p>TTL is set only on the first increment — the window starts
     * on the first request and resets after {@value} minute.
     * Subsequent requests within the window do not reset the TTL.</p>
     *
     * <p>On Redis failure the request is allowed through — failing
     * open prevents infrastructure issues from blocking users.</p>
     *
     * @param exchange  the current server exchange
     * @param chain     the remaining filter chain
     * @param redisKey  the Redis key for this user's counter
     * @param limit     the maximum requests allowed in this window
     * @param username  the authenticated user — for logging only
     * @return a {@link Mono} that completes when processing finishes
     */
    private Mono<Void> checkAndTrack(@NonNull ServerWebExchange exchange,
                                     @NonNull GatewayFilterChain chain,
                                     String redisKey,
                                     int limit,
                                     String username) {

        return redisTemplate
                .opsForValue()
                .increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(redisKey, WINDOW_DURATION)
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                }).flatMap(count -> {
                    if (count > limit) {
                        log.warn("[RATE] Request blocked | user={} count={} limit={}",
                                username, count, limit);
                        return reject(exchange);
                    }
                    log.debug("[RATE] Request allowed | user={} count={} limit={}",
                            username, count, limit);
                    return chain.filter(exchange);
                }).onErrorResume(e -> {
                    log.error("[RATE] Redis error | user={} reason={} — allowing request",
                            username, e.getMessage());
                    return chain.filter(exchange);
                });

    }

    /**
     * Determines the rate limit for the given risk level.
     *
     * <p>Defaults to {@value LOW_LIMIT} if risk level is null or
     * unrecognised — safe default for public routes and edge cases
     * where {@code ContextualRiskFilter} was bypassed.</p>
     *
     * @param riskLevel the value of the {@code X-Risk-Level} header
     * @return the maximum number of requests allowed per minute
     */
    private int getRateLimit(String riskLevel) {
        if (riskLevel == null) {
            return LOW_LIMIT;
        }
        return switch (riskLevel) {
            case "HIGH"   -> HIGH_LIMIT;
            case "MEDIUM" -> MEDIUM_LIMIT;
            default       -> LOW_LIMIT;
        };
    }

    /**
     * Rejects the request with 429 Too Many Requests.
     *
     * @param exchange the current server exchange
     * @return a {@link Mono} that completes after writing the response
     */
    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }
}
