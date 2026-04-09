package com.moodflix.gateway.routing.filter;

import com.moodflix.gateway.common.GatewayHeaders;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * First filter in the gateway chain (order = -4).
 *
 * <p>Strips headers from every incoming request that should only
 * ever be set by the gateway itself — never by external clients.
 * This prevents header injection attacks where an attacker
 * crafts headers like {@code X-Username: admin} or
 * {@code X-Risk-Level: LOW} to bypass security controls.</p>
 *
 * <p>Runs before all other filters — including {@code JwtAuthFilter}
 * — so that by the time trusted headers are injected by the gateway,
 * any attacker-supplied versions have already been removed.</p>
 *
 * <p>Applies to every request including public routes — an attacker
 * does not need to be authenticated to attempt header injection.</p>
 *
 * <p>Filter execution order:</p>
 * <pre>
 *   InboundHeaderSanitizationFilter (-4) → ..
 * </pre>
 */
@Slf4j
@Component
public final class InboundHeaderSanitizationFilter implements GlobalFilter, Ordered {

    /**
     * Headers that must be stripped from every incoming client request.
     * These headers are injected exclusively by the gateway — any
     * client-supplied values are considered malicious and removed.
     */
    private static final List<String> HEADERS_TO_STRIP = List.of(
            GatewayHeaders.USERNAME,
            GatewayHeaders.USER_TYPE,
            GatewayHeaders.RISK_LEVEL,
            GatewayHeaders.RISK_SCORE,
            GatewayHeaders.RISK_ACTION,
            GatewayHeaders.USER_VECTOR,
            GatewayHeaders.CORRELATION_ID,
            GatewayHeaders.FORWARDED_FOR,
            GatewayHeaders.REAL_IP
    );

    /** Paths that allow unauthenticated (guest) access. */
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("/api/search");

    @Override
    public int getOrder() {
        return -4;
    }

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        var path = exchange.getRequest().getPath().toString();
        var authHeader = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.AUTHORIZATION);
        boolean isGuest = isPublic(path) && (authHeader == null || !authHeader.startsWith("Bearer "));

        var mutated = exchange.mutate()
                .request(r -> r.headers(headers -> {
                    HEADERS_TO_STRIP.forEach(header -> {
                        if (headers.containsHeader(header)) {
                            log.debug("[SANITIZE] Stripped header={} path={}", header, path);
                            headers.remove(header);
                        }
                    });

                    if (isGuest) {
                        headers.set(GatewayHeaders.USER_TYPE, GatewayHeaders.USER_TYPE_GUEST);
                        log.debug("[SANITIZE] Injected X-User-Type=guest | path={}", path);
                    }
                }))
                .build();

        return chain.filter(mutated);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
