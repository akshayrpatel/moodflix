package com.moodflix.interactions.service.vector.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;

/**
 * Development implementation of {@link MovieVectorProvider}.
 * Generates a consistent, deterministic 384-dimensional unit vector from a movie ID seed.
 * Enables full end-to-end testing of the pipeline without requiring ChromaDB to be running.
 */
@Slf4j
@Component
public class DeterministicMovieVectorProvider implements MovieVectorProvider {

    private static final int DIMENSION = 384;

    @Override
    public Optional<float[]> getMovieVector(String movieId) {
        if (movieId == null || movieId.isBlank()) {
            return Optional.empty();
        }

        // Seed with movie ID hash code for deterministic reproducibility
        long seed = movieId.hashCode();
        Random random = new Random(seed);

        float[] vector = new float[DIMENSION];
        double sumSquares = 0.0;

        for (int i = 0; i < DIMENSION; i++) {
            vector[i] = (float) (random.nextGaussian());
            sumSquares += vector[i] * vector[i];
        }

        // Normalize to unit length
        double norm = Math.sqrt(sumSquares);
        for (int i = 0; i < DIMENSION; i++) {
            vector[i] = (float) (vector[i] / norm);
        }

        log.debug("Generated deterministic vector for movieId: {}", movieId);
        return Optional.of(vector);
    }
}