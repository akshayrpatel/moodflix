package com.moodflix.gateway.auth.filter;

import com.moodflix.gateway.auth.model.AuthenticatedPrincipal;
import com.moodflix.gateway.auth.service.JwtService;
import com.moodflix.gateway.common.GatewayHeaders;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * First filter in the gateway chain (order = -2).
 *
 * <p>Enforces JWT-based authentication on all non-public routes.
 * Validates the incoming {@code Authorization: Bearer <token>} header,
 * extracts the verified identity, and injects {@code X-Username} into
 * the request for downstream filters to consume.</p>
 *
 * <p>The {@code Authorization} header is stripped after validation —
 * downstream services never see the raw JWT. This establishes a
 * zero-trust boundary at the gateway edge.</p>
 *
 * <p>Public routes ({@code /auth/**}) bypass this filter entirely.</p>
 *
 * <p>Filter execution order:</p>
 * <pre>
 *   InboundHeaderSanitizationFilter (-4) → CorrelationIdFilter (-3) → JwtAuthFilter (-2) → ..
 * </pre>
 */
@Slf4j
@Component
public final class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public int getOrder() {
        return -2;
    }

    /**
     * Validates the JWT token and injects the verified identity into
     * the request headers before passing to the next filter.
     *
     * <p>Three distinct failure cases are handled explicitly:</p>
     * <ul>
     *   <li>{@link ExpiredJwtException} — token is valid but past expiry</li>
     *   <li>{@link JwtException} — token is malformed or signature invalid</li>
     *   <li>{@link IllegalArgumentException} — token string is null or blank</li>
     * </ul>
     *
     * <p>All three return {@code 401 Unauthorized} — the caller receives
     * no information about which specific check failed, to avoid
     * leaking token validation internals.</p>
     *
     * @param exchange the current server exchange
     * @param chain    the remaining filter chain
     * @return a {@link Mono} that completes when processing finishes
     */
    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // Auth routes — bypass entirely
        if (path.startsWith("/auth/")) {
            return chain.filter(exchange);
        }

        // Guest on a public path — already tagged by InboundHeaderSanitizationFilter
        if (GatewayHeaders.USER_TYPE_GUEST.equals(exchange.getRequest().getHeaders().getFirst(GatewayHeaders.USER_TYPE))) {
            return chain.filter(exchange);
        }

        String authHeader = exchange
                .getRequest()
                .getHeaders()
                .getFirst(GatewayHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JWT] Missing or malformed Authorization header | path={}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        try {
            String token = authHeader.substring(7);
            AuthenticatedPrincipal authPrincipal = jwtService.validate(token);
            String username = authPrincipal.username();

            log.debug("[JWT] Valid token | user={} | path={}", username, path);
            ServerWebExchange mutatedExchange = exchange
                    .mutate()
                    .request(request -> request
                            .header(GatewayHeaders.USERNAME, username)
                            .header(GatewayHeaders.USER_TYPE, GatewayHeaders.USER_TYPE_AUTHENTICATED)
                            .headers(headers -> headers.remove(GatewayHeaders.AUTHORIZATION)))
                    .build();
            return chain.filter(mutatedExchange);

        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token expired | path={}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        } catch (JwtException e) {
            log.warn("[JWT] Token invalid or tampered | path={} | reason={}", path, e.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] Token string malformed | path={}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Writes the given HTTP status to the response and terminates
     * the filter chain. Used for all rejection paths in this filter.
     *
     * @param exchange the current server exchange
     * @param status   the HTTP status to return to the caller
     * @return a {@link Mono} that completes after writing the response
     */
    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

}
