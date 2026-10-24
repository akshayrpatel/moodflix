package com.moodflix.interactions.service.vector;

import com.moodflix.interactions.service.vector.strategy.WeightedNormalizedVectorShiftStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WeightedNormalizedVectorShiftStrategy}.
 * Verifies mathematical invariants, normalization, and bounds checking.
 */
class WeightedNormalizedVectorShiftStrategyTest {

    private WeightedNormalizedVectorShiftStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new WeightedNormalizedVectorShiftStrategy();
    }

    @Test
    @DisplayName("Should produce a vector of length 384 with L2 norm equal to 1.0")
    void calculateNewVector_producesNormalizedUnitVector() {
        // Arrange: create two 384-dim unit vectors
        float[] userVector = createNormalizedVector(0.5f);
        float[] movieVector = createNormalizedVector(0.8f);

        // Act: apply LIKE interaction (+0.1)
        float[] result = strategy.calculateNewVector(userVector, movieVector, 0.1);

        // Assert
        assertNotNull(result);
        assertEquals(384, result.length);

        // Compute L2 norm: sqrt(sum(v_i^2))
        double sumSquares = 0.0;
        for (float val : result) {
            sumSquares += val * val;
        }
        double l2Norm = Math.sqrt(sumSquares);

        assertEquals(1.0, l2Norm, 1e-4, "Output vector must be strictly unit-length for cosine similarity");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when dimension is not 384")
    void calculateNewVector_throwsOnDimensionMismatch() {
        float[] wrongLengthVector = new float[128];
        float[] validVector = createNormalizedVector(1.0f);

        assertThrows(IllegalArgumentException.class, () ->
                strategy.calculateNewVector(wrongLengthVector, validVector, 0.1)
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when either vector is null")
    void calculateNewVector_throwsOnNullInput() {
        float[] validVector = createNormalizedVector(1.0f);

        assertThrows(IllegalArgumentException.class, () ->
                strategy.calculateNewVector(null, validVector, 0.1)
        );
        assertThrows(IllegalArgumentException.class, () ->
                strategy.calculateNewVector(validVector, null, 0.1)
        );
    }

    // Helper to generate a valid 384-dim normalized vector
    private float[] createNormalizedVector(float seed) {
        float[] vec = new float[384];
        Arrays.fill(vec, seed);
        double sumSquares = 384 * (seed * seed);
        double norm = Math.sqrt(sumSquares);
        for (int i = 0; i < 384; i++) {
            vec[i] = (float) (vec[i] / norm);
        }
        return vec;
    }
}