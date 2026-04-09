package com.moodflix.gateway.onboarding.service;

import com.moodflix.gateway.auth.repository.UserRepository;
import com.moodflix.gateway.common.GatewayHeaders;
import com.moodflix.gateway.onboarding.model.MoodSelection;
import com.moodflix.gateway.onboarding.model.OnboardingResponse;
import com.moodflix.gateway.onboarding.model.SeedVectorResponse;
import com.moodflix.gateway.profile.cache.VectorCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Orchestrates the mood-based onboarding flow.
 *
 * <p>Sends mood selections to the FastAPI AI engine,
 * receives a seed taste vector, persists it to PostgreSQL,
 * and warms the Redis cache so the first personalized
 * request is immediately fast.</p>
 */
@Slf4j
@Service
public class OnboardingServiceImpl implements OnboardingService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final VectorCacheService cacheService;

    public OnboardingServiceImpl(
            @Qualifier("aiEngineWebClient") WebClient webClient,
            UserRepository userRepository,
            VectorCacheService vectorCacheService) {
        this.webClient          = webClient;
        this.userRepository     = userRepository;
        this.cacheService = vectorCacheService;
    }

    public Mono<OnboardingResponse> complete(String username, MoodSelection moodSelection) {
        log.debug("[ONBOARDING] Starting | user={}", username);
        return onBoardUser(username, moodSelection)
                .flatMap(response -> persistSeedVector(username, response.seedVector()).thenReturn(response))
                .map(response -> new OnboardingResponse("Onboarding Complete", response.confidence()))
                .doOnSuccess(r -> log.debug("[ONBOARDING] Complete | user={} confidence={}", username, r.vectorConfidence()))
                .doOnError(r -> log.error("[ONBOARDING] Failed | user={}", username));
    }

    private Mono<SeedVectorResponse> onBoardUser(String username, MoodSelection moodSelection) {
        return webClient.post()
                .uri("/api/seed-vector")
                .header(GatewayHeaders.USERNAME, username)
                .bodyValue(moodSelection)
                .retrieve()
                .bodyToMono(SeedVectorResponse.class)
                .doOnError(e -> log.error("[ONBOARDING] FastAPI call failed | user={} reason={}",
                        username, e.getMessage()));
    }

    private Mono<Void> persistSeedVector(String username, float[] vector) {
        return userRepository.findByUsername(username)
                .flatMap(user -> {
                    user.setTasteVector(vector);
                    return userRepository.save(user);
                })
                .doOnSuccess(user -> log.debug("[ONBOARDING] Vector saved to PostgreSQL | user={}", username))
                .doOnError(e -> log.error("[ONBOARDING] PostgreSQL save failed | user={} reason={}", username, e.getMessage()))
                .flatMap(user -> cacheService.save(username, vector))
                .doOnSuccess(v -> log.debug("[ONBOARDING] Cache warmed | user={}", username))
                .then();
    }

}
