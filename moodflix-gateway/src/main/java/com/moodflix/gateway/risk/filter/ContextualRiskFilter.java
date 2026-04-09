package com.moodflix.gateway.risk.filter;

import com.moodflix.gateway.common.GatewayHeaders;
import com.moodflix.gateway.risk.cache.SessionCache;
import com.moodflix.gateway.risk.model.AccessPolicyResult;
import com.moodflix.gateway.risk.model.SessionContext;
import com.moodflix.gateway.risk.model.RequestContext;
import com.moodflix.gateway.risk.service.evaluator.RiskEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Second filter in the gateway chain (order = -1).
 *
 * <p>Implements continuous authentication by evaluating whether
 * the current request is consistent with the user's established
 * session behaviour. Every authenticated request is checked —
 * not just login.</p>
 *
 * <p>Delegates storage to {@link SessionCache} and risk decisions
 * to {@link RiskEvaluator} — this filter only handles the
 * request/response lifecycle and acts on verdicts.</p>
 *
 * <p>Filter execution order:</p>
 * <pre>
 *   InboundHeaderSanitizationFilter (-4) → CorrelationIdFilter (-3) → JwtAuthFilter (-2)
 *   → ContextualRiskFilter (-1) → ..
 * </pre>
 */
@Slf4j
@Component
public final class ContextualRiskFilter implements GlobalFilter, Ordered {

    private final SessionCache sessionCache;
    private final RiskEvaluator riskEvaluator;

    /**
     * @param sessionCache  the session storage abstraction —
     *                      decoupled from Redis directly
     * @param riskEvaluator the risk evaluation abstraction —
     *                      decoupled from scoring logic directly
     */
    public ContextualRiskFilter(SessionCache sessionCache, RiskEvaluator riskEvaluator) {
        this.sessionCache = sessionCache;
        this.riskEvaluator = riskEvaluator;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    /**
     * Validates the current request against the user's session history
     * and acts on the risk verdict before passing to the next filter.
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

        RequestContext requestContext = new RequestContext(
                username,
                extractIp(exchange),
                extractUserAgent(exchange),
                Instant.now()
        );

        return sessionCache.load(username)
                .flatMap(sessionContext -> evaluateAndAct(exchange, chain, sessionContext, requestContext));
    }

    /**
     * Acts on the risk verdict returned by {@link RiskEvaluator}.
     *
     * <p>HIGH risk terminates the request immediately with 403.
     * MEDIUM and LOW risk enrich the exchange with risk headers,
     * persist the updated session, and continue the chain.</p>
     *
     * @param exchange  the current server exchange
     * @param chain     the remaining filter chain
     * @param stored    the session state loaded from storage
     * @param incoming  the current request context
     * @return a {@link Mono} that completes when processing finishes
     */
    private Mono<Void> evaluateAndAct(@NonNull ServerWebExchange exchange,
                                      @NonNull GatewayFilterChain chain,
                                      SessionContext stored,
                                      RequestContext incoming) {

        AccessPolicyResult accessPolicyResult = riskEvaluator.evaluate(stored, incoming);

        if (accessPolicyResult.isBlocked()) {
            log.error("[RISK] Blocking request | user={} score={} anomalies={}",
                    incoming.username(),
                    accessPolicyResult.evaluationScore(),
                    accessPolicyResult.anomalies().stream()
                            .map(a -> a.type().name())
                            .toList());
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        SessionContext updatedSession = buildUpdatedSession(stored, incoming, accessPolicyResult);
        ServerWebExchange enrichedExchange = enrichExchange(exchange, accessPolicyResult);

        return sessionCache.save(incoming.username(), updatedSession)
                .then(chain.filter(enrichedExchange));
    }

    /**
     * Builds an updated {@link SessionContext} after evaluation.
     */
    private SessionContext buildUpdatedSession(
            SessionContext previous,
            RequestContext incoming,
            AccessPolicyResult accessPolicyResult
    ) {
        int previousAnomalies = previous.isNew() ? 0 : previous.getConsecutiveAnomalies();
        int updatedAnomalies  = accessPolicyResult.anomalies().isEmpty()
                ? 0
                : previousAnomalies + 1;
        return SessionContext.builder()
                .username(incoming.username())
                .lastKnownIp(incoming.ipAddress())
                .lastKnownUserAgent(incoming.userAgent())
                .lastRequestTime(incoming.requestTime())
                .riskScore(accessPolicyResult.evaluationScore())
                .consecutiveAnomalies(updatedAnomalies)
                .build();
    }

    /**
     * Mutates the exchange to inject risk signal headers downstream.
     **/
    private ServerWebExchange enrichExchange(ServerWebExchange exchange, AccessPolicyResult accessPolicyResult) {
        return exchange.mutate()
                .request(r -> r
                        .header(GatewayHeaders.RISK_LEVEL, accessPolicyResult.riskLevel().name())
                        .header(GatewayHeaders.RISK_SCORE, String.valueOf(accessPolicyResult.evaluationScore()))
                        .header(GatewayHeaders.RISK_ACTION, accessPolicyResult.action().name())
                ).build();
    }

    /**
     * Extracts the real client IP from the request.
     *
     * <p>Checks {@code X-Forwarded-For} first to handle requests
     * arriving through a load balancer or reverse proxy. The header
     * can contain a comma-separated chain — the first entry is always
     * the original client IP.</p>
     *
     * @param exchange the current server exchange
     * @return the client IP, or {@code "unknown"} if not resolvable
     */
    private String extractIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";
    }

    /**
     * Extracts the User-Agent string from the request headers.
     *
     * @param exchange the current server exchange
     * @return the User-Agent string, or {@code "unknown"} if not present
     */
    private String extractUserAgent(ServerWebExchange exchange) {
        String ua = exchange.getRequest()
                .getHeaders()
                .getFirst("User-Agent");
        return ua != null ? ua : "unknown";
    }

    /**
     * Writes the given HTTP status to the response and terminates
     * the filter chain.
     *
     * @param exchange the current server exchange
     * @param status   the HTTP status to return
     * @return a {@link Mono} that completes after writing the response
     */
    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}