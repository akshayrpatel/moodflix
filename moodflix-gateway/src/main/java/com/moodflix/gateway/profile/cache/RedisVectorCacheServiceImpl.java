package com.moodflix.gateway.profile.cache;

import com.moodflix.gateway.auth.model.User;
import com.moodflix.gateway.auth.repository.UserRepository;
import com.moodflix.gateway.common.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Redis-backed implementation of {@link VectorCacheService}.
 *
 * <p>Implements cache-aside for reads and write-through for saves.
 * Assumes Redis is available — simplified for development.</p>
 */
@Slf4j
@Component
public final class RedisVectorCacheServiceImpl implements VectorCacheService {

    private final ReactiveRedisTemplate<String, float[]> redisTemplate;
    private final UserRepository userRepository;

    /**
     * @param redisTemplate  the typed template for float[] vectors
     * @param userRepository the repository for PostgreSQL fallback
     */
    public RedisVectorCacheServiceImpl(
            @Qualifier("vectorRedisTemplate") ReactiveRedisTemplate<String, float[]> redisTemplate,
            UserRepository userRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the taste vector using cache-aside pattern.
     *
     * <p>Checks Redis first. On miss falls back to PostgreSQL
     * and populates the cache. Returns empty if the user has
     * no vector yet or new users who have not completed onboarding.</p>
     */
    @Override
    public Mono<float[]> get(String username) {
        String redisKey = buildKey(username);
        return redisTemplate.opsForValue()
                .get(redisKey)
                .doOnNext(v -> log.debug("[VECTOR CACHE] Hit | user={}", username))
                .switchIfEmpty(Mono.defer(() -> fetchFromDbAndCache(username)));
    }

    /**
     * Saves a vector for the given user.
     */
    @Override
    public Mono<Void> save(String username, float[] vector) {
        return redisTemplate.opsForValue()
            .set(buildKey(username), vector, RedisKeys.VECTOR_TTL)
            .then();
    }

    /**
     * Invalidates the cached vector for the given user.
     */
    @Override
    public Mono<Void> invalidate(String username) {
        return redisTemplate.delete(buildKey(username))
                .doOnNext(count -> log.debug("[VECTOR CACHE] Invalidated | user={} deletedCount={}", username, count))
                .then();
    }

    private String buildKey(String username) {
        return RedisKeys.VECTOR_CACHE + username;
    }

    private Mono<float[]> fetchFromDbAndCache(String username) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getTasteVector() != null)
                .map(User::getTasteVector)
                .flatMap(tasteVector -> save(username, tasteVector).thenReturn(tasteVector))
                .doOnNext(v -> log.debug("[VECTOR CACHE] Miss | Populated from DB for user={}", username));
    }
}
