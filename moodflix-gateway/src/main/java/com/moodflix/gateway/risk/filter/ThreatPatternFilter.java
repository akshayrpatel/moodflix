package com.moodflix.gateway.risk.filter;

import com.moodflix.gateway.common.GatewayHeaders;
import com.moodflix.gateway.risk.model.BehavioralAnomaly;
import com.moodflix.gateway.risk.model.ResponseContext;
import com.moodflix.gateway.risk.model.RiskLevel;
import com.moodflix.gateway.risk.service.event.RiskEventEmitter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Fourth filter in the gateway chain (order = 1).
 *
 * <p>Detects suspicious behavioural threat patterns by observing
 * response patterns across multiple requests over time. Unlike
 * {@code ContextualRiskFilter} which evaluates a single request
 * against session history, this filter detects patterns that only
 * become visible across many requests — repeated auth failures,
 * burst traffic, and repeated forbidden access attempts.</p>
 *
 * <p>Runs AFTER the response is written — it needs the HTTP status
 * code to detect patterns like repeated 401s and 403s. Uses sliding
 * time windows stored in Redis to track per-user counters that reset
 * automatically via TTL.</p>
 *
 * <p>This filter observes only — it never blocks requests. Anomalies
 * are emitted via {@link RiskEventEmitter} for logging and future
 * Kafka integration. Blocking based on these signals is a future
 * enhancement.</p>
 *
 * <p>Filter execution order:</p>
 * <pre>
 *   InboundHeaderSanitizationFilter (-4) → CorrelationIdFilter (-3) → JwtAuthFilter (-2)
 *   → ContextualRiskFilter (-1) → VectorHeaderFilter (0) → ThreatPatternFilter (1) -> ..
 * </pre>
 */
@Slf4j
@Component
public final class ThreatPatternFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RiskEventEmitter eventEmitter;

    public ThreatPatternFilter(ReactiveRedisTemplate<String, String> redisTemplate, RiskEventEmitter eventEmitter){
        this.redisTemplate = redisTemplate;
        this.eventEmitter = eventEmitter;
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        String username = exchange.getRequest()
                .getHeaders()
                .getFirst(GatewayHeaders.USERNAME);

        if (username == null || username.isBlank()) {
            return chain.filter(exchange);
        }

        return chain.filter(exchange)
                .then(Mono.defer(() -> buildResponseContext(exchange, username)
                        .flatMap(this::observeResponse)));
    }

    /**
     * Builds a {@link ResponseContext} from the completed exchange.
     *
     * <p>Returns empty if status code is unavailable — edge case
     * where connection was dropped before response was written.</p>
     *
     * @param exchange the completed server exchange
     * @param username the authenticated user
     * @return a {@link Mono} emitting the response context or empty
     */
    private Mono<ResponseContext> buildResponseContext(ServerWebExchange exchange, String username) {

        var statusCode = exchange.getResponse().getStatusCode();
        if (statusCode == null) {
            return Mono.empty();
        }

        return Mono.just(new ResponseContext(
                username,
                statusCode.value(),
                exchange.getRequest().getPath().toString(),
                Instant.now()
        ));
    }

    /**
     * Observes the response context and tracks threat patterns.
     *
     * <p>Always tracks burst — every request counts toward the
     * burst window. Additionally, tracks 401 or 403 patterns
     * when those status codes are observed.</p>
     *
     * @param context the observed response context
     * @return a {@link Mono} that completes when tracking finishes
     */
    private Mono<Void> observeResponse(ResponseContext context) {
        Mono<Void> trackBurst = incrementAndCheck(
                ThreatWindowConfig.forBurst(context.username()),
                context
        );

        if (context.statusCode() == 401) {
            return Mono.when(
                    trackBurst,
                    incrementAndCheck(ThreatWindowConfig.for401(context.username()), context)
            );
        }

        if (context.statusCode() == 403) {
            return Mono.when(
                    trackBurst,
                    incrementAndCheck(ThreatWindowConfig.for403(context.username()), context)
            );
        }

        return trackBurst;
    }

    /**
     * Increments a Redis counter and emits an anomaly if the
     * threshold is crossed.
     *
     * <p>TTL is set only on the first increment — defines the
     * observation window. Counter resets automatically when
     * the TTL expires.</p>
     *
     * @param config  the threat window configuration
     * @param context the observed response context
     * @return a {@link Mono} that completes when the check finishes
     */
    private Mono<Void> incrementAndCheck(ThreatWindowConfig config, ResponseContext context) {
        return redisTemplate.opsForValue()
                .increment(config.key())
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate
                                .expire(config.key(), config.ttl())
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count >= config.threshold()) {
                        BehavioralAnomaly anomaly = buildAnomaly(config, count);
                        return eventEmitter.emit(anomaly, context.username(), context.endpoint());
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error(
                            "[THREAT] Redis error | key={} user={} reason={}",
                            config.key(),
                            context.username(),
                            e.getMessage()
                    );
                    return Mono.empty();
                });
    }

    /**
     * Constructs a {@link BehavioralAnomaly} when a counter
     * threshold is crossed.
     *
     * @param config the threat window configuration
     * @param count  the current counter value
     * @return a new anomaly representing the detected pattern
     */
    private BehavioralAnomaly buildAnomaly(ThreatWindowConfig config, long count) {
        String anomalyDescription = "Threshold crossed - count=" + count + " threshold=" + config.threshold();
        return new BehavioralAnomaly(
                config.anomalyType(),
                config.level(),
                anomalyDescription,
                Instant.now()
        );
    }


    /**
     * Configuration for tracking a specific threat pattern over
     * a sliding time window in Redis.
     *
     * <p>Use static factory methods — each encapsulates the correct
     * constants for its threat type, keeping callers free of
     * configuration details.</p>
     */
    private record ThreatWindowConfig(String key,
                                      Duration ttl,
                                      int threshold,
                                      BehavioralAnomaly.AnomalyType anomalyType,
                                      RiskLevel level) {

        private static final Duration AUTH_FAILURE_TTL = Duration.ofSeconds(30);
        private static final Duration BURST_TTL = Duration.ofSeconds(10);

        private static final int THRESHOLD_401 = 3;
        private static final int THRESHOLD_403 = 3;
        private static final int THRESHOLD_BURST = 20;

        /**
         * Configuration for tracking repeated 401 auth failures.
         *
         * @param username the user being tracked
         * @return a configured {@link ThreatWindowConfig}
         */
        public static ThreatWindowConfig for401(String username) {
            return new ThreatWindowConfig(
                    "signal:401:" + username,
                    AUTH_FAILURE_TTL,
                    THRESHOLD_401,
                    BehavioralAnomaly.AnomalyType.REPEATED_AUTH_FAILURE,
                    RiskLevel.HIGH
            );
        }

        /**
         * Configuration for tracking repeated 403 forbidden responses.
         *
         * @param username the user being tracked
         * @return a configured {@link ThreatWindowConfig}
         */
        public static ThreatWindowConfig for403(String username) {
            return new ThreatWindowConfig(
                    "signal:403:" + username,
                    AUTH_FAILURE_TTL,
                    THRESHOLD_403,
                    BehavioralAnomaly.AnomalyType.REPEATED_FORBIDDEN,
                    RiskLevel.MEDIUM
            );
        }

        /**
         * Configuration for tracking burst request patterns.
         *
         * @param username the user being tracked
         * @return a configured {@link ThreatWindowConfig}
         */
        public static ThreatWindowConfig forBurst(String username) {
            return new ThreatWindowConfig(
                    "signal:burst:" + username,
                    BURST_TTL,
                    THRESHOLD_BURST,
                    BehavioralAnomaly.AnomalyType.BURST_REQUESTS,
                    RiskLevel.MEDIUM
            );
        }
    }
}
