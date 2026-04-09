package com.moodflix.gateway.onboarding.service;

import com.moodflix.gateway.onboarding.model.MoodSelection;
import com.moodflix.gateway.onboarding.model.OnboardingResponse;
import reactor.core.publisher.Mono;

public interface OnboardingService {
    public Mono<OnboardingResponse> complete(String username, MoodSelection moodSelection);
}
