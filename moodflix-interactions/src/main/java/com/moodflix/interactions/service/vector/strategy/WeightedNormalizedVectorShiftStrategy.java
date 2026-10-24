package com.moodflix.interactions.service.vector.strategy;

import org.springframework.stereotype.Component;

@Component
public class WeightedNormalizedVectorShiftStrategy implements VectorShiftStrategy {

    private static final int EXPECTED_DIMENSION = 384;
    private static final float EPSILON = 1e-9f; // Prevents division by zero

    @Override
    public float[] calculateNewVector(float[] userVector, float[] movieVector, double weight) {
        if (userVector == null || movieVector == null) {
            throw new IllegalArgumentException("User vector and movie vector must not be null");
        }
        if (userVector.length != EXPECTED_DIMENSION || movieVector.length != EXPECTED_DIMENSION) {
            throw new IllegalArgumentException(String.format(
                    "Dimension mismatch: expected %d, got user: %d, movie: %d",
                    EXPECTED_DIMENSION, userVector.length, movieVector.length));
        }

        float[] shifted = new float[EXPECTED_DIMENSION];
        double sumOfSquares = 0.0;

        // Step 1: Element-wise weighted shift: V_shifted = V_user + (weight * V_movie)
        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            shifted[i] = (float) (userVector[i] + (weight * movieVector[i]));
            sumOfSquares += shifted[i] * shifted[i];
        }

        // Step 2: Compute L2 Norm (magnitude)
        double norm = Math.sqrt(sumOfSquares);
        if (norm < EPSILON) {
            // Guard against edge case where user and movie vectors cancel out to zero
            return userVector.clone();
        }

        // Step 3: L2 Normalization (scale to unit length 1.0 for cosine similarity)
        float[] normalized = new float[EXPECTED_DIMENSION];
        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            normalized[i] = (float) (shifted[i] / norm);
        }

        return normalized;
    }
}
