package com.moodflix.interactions.service.vector.strategy;

/**
 * Strategy contract for calculating a user's updated taste vector
 * based on an interaction with a movie embedding.
 */
public interface VectorShiftStrategy {

    /**
     * Calculates the new normalized taste vector.
     *
     * @param userVector  current user taste vector (length 384)
     * @param movieVector embedding of the movie interacted with (length 384)
     * @param weight      interaction impact (+0.1 for LIKE, -0.2 for DISLIKE, etc.)
     * @return new unit-length vector of the same dimension
     */
    float[] calculateNewVector(float[] userVector, float[] movieVector, double weight);
}
