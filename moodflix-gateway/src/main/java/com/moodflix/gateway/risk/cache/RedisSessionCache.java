package com.moodflix.gateway.risk.cache;

import com.moodflix.gateway.risk.model.SessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis-backed implementation of {@link SessionCache}.
 *
 * <p>Stores session state as JSON using the typed
 * {@code ReactiveRedisTemplate<String, SessionContext>} configured
 * in {@code RedisConfig}. Keys follow the pattern
 * {@code session:ctx:{username}}.</p>
 *
 * <p>All Redis failures are logged and swallowed — storage errors
 * must never propagate to the user as failed requests.</p>
 */
@Slf4j
@Component
public final class RedisSessionCache implements SessionCache {

    private static final String KEY_PREFIX  = "session:ctx:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final ReactiveRedisTemplate<String, SessionContext> redisTemplate;

    /**
     * @param redisTemplate the typed Redis template for session storage —
     *                      qualified to distinguish from the default
     *                      {@code String} template auto-configured by Spring Boot
     */
    public RedisSessionCache(
            @Qualifier("sessionContextRedisTemplate")
            ReactiveRedisTemplate<String, SessionContext> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<SessionContext> load(String username) {
        String key = KEY_PREFIX + username;

        return redisTemplate.opsForValue()
                .get(key)
                .defaultIfEmpty(SessionContext.empty())
                .doOnError(e -> log.error(
                        "[SESSION] Failed to load session | user={} | reason={}",
                        username, e.getMessage()))
                .onErrorReturn(SessionContext.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> save(String username, SessionContext context) {
        String key = KEY_PREFIX + username;

        return redisTemplate.opsForValue()
                .set(key, context, SESSION_TTL)
                .doOnError(e -> log.error(
                        "[SESSION] Failed to save session | user={} | reason={}",
                        username, e.getMessage()))
                .onErrorComplete()
                .then();
    }
}