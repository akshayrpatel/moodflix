package com.moodflix.gateway.onboarding.model;

/**
 * Response returned to the frontend after successful onboarding.
 *
 * <p>The frontend uses {@code vectorConfidence} to decide whether
 * to prompt the user to refine their selections — a low confidence
 * score means the mood selections were ambiguous.</p>
 *
 * @param message           success message
 * @param vectorConfidence  confidence score from FastAPI
 */
public record OnboardingResponse(
        String message,
        double vectorConfidence
) {}