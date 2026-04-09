package com.moodflix.gateway.profile.cache;

import reactor.core.publisher.Mono;

/**
 * Contract for storing and retrieving user taste vectors.
 */
public interface VectorCacheService {

    /**
     * Retrieves the taste vector for the given user.
     *
     * <p>Returns empty if the user has no vector yet —
     * new users who have not completed onboarding.</p>
     *
     * @param username the authenticated user
     * @return a {@link Mono} emitting the vector or empty
     */
    Mono<float[]> get(String username);

    /**
     * Saves a taste vector for the given user.
     *
     * <p>Implementations must persist to the primary store
     * and update the cache atomically.</p>
     *
     * @param username the authenticated user
     * @param vector   the taste vector to save
     * @return a {@link Mono} that completes when save finishes
     */
    Mono<Void> save(String username, float[] vector);

    /**
     * Invalidates the cached vector for the given user.
     *
     * <p>Called after async vector updates — ensures next
     * request fetches the fresh value from primary store.</p>
     *
     * @param username the authenticated user
     * @return a {@link Mono} that completes when invalidation finishes
     */
    Mono<Void> invalidate(String username);
}