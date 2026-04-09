package com.moodflix.gateway.onboarding.model;

/**
 * Seed vector response from the FastAPI AI engine.
 *
 * <p>Contains the computed taste vector from the user's mood
 * selections and a confidence score indicating how strongly
 * the selections map to a specific taste region.</p>
 *
 * @param seedVector  the computed float array taste vector
 * @param confidence  0.0 to 1.0 — how confident the seed is
 */
public record SeedVectorResponse(
        float[] seedVector,
        double confidence
) {}