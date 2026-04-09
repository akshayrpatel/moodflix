package com.moodflix.gateway.routing.filter;

import com.moodflix.gateway.common.GatewayHeaders;
import com.moodflix.gateway.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Third filter in the gateway chain (order = 0).
 *
 * <p>Enriches authenticated requests with the user's personalised
 * taste vector fetched from PostgreSQL. The vector is injected as
 * {@code X-User-Vector} so the FastAPI AI engine can personalise
 * recommendations without making its own database call.</p>
 *
 * <p>Three cases where no vector is injected:</p>
 * <ul>
 *   <li>No {@code X-Username} header — public route, skip entirely</li>
 *   <li>User not found in database — deleted account edge case</li>
 *   <li>Taste vector is null — new user who has not completed onboarding</li>
 * </ul>
 *
 * <p>On any database error the request continues without a vector —
 * a vector fetch failure must never block the user's request.</p>
 *
 * <p>Filter execution order:</p>
 * <pre>
 *   InboundHeaderSanitizationFilter (-4) → CorrelationIdFilter (-3) → JwtAuthFilter (-2)
 *   → ContextualRiskFilter (-1) → VectorHeaderFilter (0) → ..
 * </pre>
 */
@Slf4j
@Component
public final class VectorHeaderFilter implements GlobalFilter, Ordered {

    private final UserRepository userRepository;

    public VectorHeaderFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * Fetches the user's taste vector and injects it into the
     * request headers before passing to the next filter.
     *
     * <p>Reads {@code X-Username} injected by {@code JwtAuthFilter} —
     * never parses the JWT directly. All three skip cases complete
     * normally without blocking the request.</p>
     *
     * @param exchange the current server exchange
     * @param chain    the remaining filter chain
     * @return a {@link Mono} that completes when processing finishes
     */
    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        String username = exchange.getRequest()
                .getHeaders()
                .getFirst(GatewayHeaders.USERNAME);

        if (username == null || username.isBlank()) {
            return chain.filter(exchange);
        }

        return userRepository.findByUsername(username)
                .flatMap(user -> {
                    if (user.getTasteVector() == null) {
                        log.debug("[VECTOR] No taste vector | user={}", username);
                        return chain.filter(exchange);
                    }

                    float[] vec = user.getTasteVector();
                    var sb = new StringBuilder(vec.length * 8);
                    for (int i = 0; i < vec.length; i++) {
                        if (i > 0) sb.append(',');
                        sb.append(vec[i]);
                    }
                    String vectorString = sb.toString();
                    log.debug("[VECTOR] Injecting vector | user={}", username);

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(r -> r.header(GatewayHeaders.USER_VECTOR, vectorString))
                            .build();

                    return chain.filter(mutatedExchange);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("[VECTOR] User not found | user={}", username);
                    return chain.filter(exchange);
                }))
                .onErrorResume(e -> {
                    log.error("[VECTOR] Failed to fetch vector | user={} reason={}", username, e.getMessage());
                    return chain.filter(exchange);
                });
    }
}
