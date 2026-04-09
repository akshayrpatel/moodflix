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

import java.util.UUID;

/**
 * First filter in the gateway chain (order = -3).
 *
 * <p>Ensures every request carries a unique {@code X-Correlation-ID}
 * header that flows through the entire filter chain, into downstream
 * services, and back to the client in the response. This enables
 * distributed tracing — every log line across every service can be
 * tied back to a single originating request.</p>
 *
 * <p>If the request already carries a correlation ID — for example
 * from a trusted internal service making a downstream call — the
 * existing ID is preserved and echoed back in the response.</p>
 *
 * <p>Filter execution order:</p>
 * <pre>
 *   InboundHeaderSanitizationFilter (-4) → CorrelationIdFilter (-3) → ..
 * </pre>
 */
@Slf4j
@Component
public final class CorrelationIdFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return -3;
    }

    /**
     * Injects a correlation ID into both the request and response headers.
     *
     * <p>If {@code X-Correlation-ID} is already present in the incoming
     * request it is preserved — the existing ID is echoed into the response
     * without generating a new one. This preserves traceability for requests
     * arriving from internal services that already carry a trace context.</p>
     *
     * <p>If absent a new UUID is generated, injected into the mutated
     * request for downstream filters, and added to the response so the
     * client can correlate their request with gateway logs.</p>
     *
     * @param exchange the current server exchange
     * @param chain    the remaining filter chain
     * @return a {@link Mono} that completes when processing finishes
     */
    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {

        String existingId = exchange.getRequest()
                .getHeaders()
                .getFirst(GatewayHeaders.CORRELATION_ID);
        String correlationId = existingId != null ? existingId : UUID.randomUUID().toString();

        exchange.getResponse().getHeaders().add(GatewayHeaders.CORRELATION_ID, correlationId);
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header(GatewayHeaders.CORRELATION_ID, correlationId))
                .build();

        return chain.filter(mutated);
    }
}
