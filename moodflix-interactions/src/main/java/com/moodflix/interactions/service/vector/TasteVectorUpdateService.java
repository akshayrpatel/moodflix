package com.moodflix.interactions.service.vector;

import com.moodflix.interactions.model.InteractionEvent;
import com.moodflix.interactions.model.User;
import com.moodflix.interactions.repository.UserRepository;
import com.moodflix.interactions.service.vector.provider.MovieVectorProvider;
import com.moodflix.interactions.service.vector.strategy.VectorShiftStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Core application service that coordinates user taste-vector updates.
 * <p>
 * Orchestrates user retrieval, embedding lookup, mathematical vector shift,
 * database persistence with optimistic concurrency control, and Gateway cache invalidation.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TasteVectorUpdateService {

    private static final String GATEWAY_VECTOR_CACHE_PREFIX = "vector:taste:";

    private final UserRepository userRepository;
    private final MovieVectorProvider movieVectorProvider;
    private final VectorShiftStrategy vectorShiftStrategy;
    private final StringRedisTemplate redisTemplate;

    /**
     * Applies a user interaction to update the persistent taste vector.
     *
     * @param event the interaction event payload
     * @throws IllegalArgumentException if user or movie vector cannot be found
     * @throws org.springframework.dao.OptimisticLockingFailureException if a concurrent update modified the user row
     */
    @Transactional
    public void processInteraction(InteractionEvent event) {
        UUID userId = event.userId();
        String movieId = event.movieId();
        double weight = event.weight();

        log.debug("Processing interaction for userId: {}, movieId: {}, weight: {}", userId, movieId, weight);

        // 1. Fetch user from PostgreSQL
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // 2. Fetch pre-computed movie embedding from AI Engine
        float[] movieVector = movieVectorProvider.getMovieVector(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie embedding not found for movieId: " + movieId));

        // 3. Handle cold-start edge case: if user has no vector, seed with movie vector directly
        float[] currentUserVector = user.getTasteVector();
        float[] updatedVector;
        if (currentUserVector == null || currentUserVector.length == 0) {
            log.info("User {} has no taste vector. Initializing with movie vector for {}", userId, movieId);
            updatedVector = movieVector.clone();
        } else {
            // Apply weighted vector shift + L2 normalization
            updatedVector = vectorShiftStrategy.calculateNewVector(currentUserVector, movieVector, weight);
        }

        // 4. Persist updated vector to PostgreSQL (Hibernate checks and increments @Version)
        user.setTasteVector(updatedVector);
        userRepository.save(user);
        log.info("Successfully updated taste vector in PostgreSQL for userId: {} (new version: {})", userId, user.getVersion());

        // 5. Invalidate Gateway Redis Cache so future searches read the fresh vector
        invalidateGatewayCache(user.getUsername());
    }

    private void invalidateGatewayCache(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String cacheKey = GATEWAY_VECTOR_CACHE_PREFIX + username;
        try {
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated Gateway vector cache for key: {}", cacheKey);
        } catch (Exception ex) {
            // Non-critical: Redis cache expiry (1 hr) acts as fallback if eviction fails
            log.warn("Failed to invalidate Gateway cache key: {}. Gateway will serve stale cache until TTL.", cacheKey, ex);
        }
    }
}