package com.moodflix.gateway.onboarding.controller;

import com.moodflix.gateway.auth.service.JwtService;
import com.moodflix.gateway.onboarding.model.MoodSelection;
import com.moodflix.gateway.onboarding.model.OnboardingResponse;
import com.moodflix.gateway.onboarding.service.OnboardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Handles the mood-based onboarding flow.
 *
 * <p>Receives mood selections from the frontend and delegates
 * to {@link OnboardingService} to compute and persist the
 * seed taste vector.</p>
 *
 * <p>This controller handles requests directly (not through
 * gateway routing), so it validates the JWT from the
 * {@code Authorization} header itself rather than relying
 * on {@code JwtAuthFilter}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/onboarding")
public class OnBoardingController {

    private final OnboardingService onboardingService;
    private final JwtService jwtService;

    public OnBoardingController(OnboardingService onboardingService, JwtService jwtService) {
        this.onboardingService = onboardingService;
        this.jwtService = jwtService;
    }

    @PostMapping("/complete")
    public Mono<OnboardingResponse> complete(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MoodSelection moodSelection) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }

        String username;
        try {
            username = jwtService.validate(authHeader.substring(7)).username();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        log.debug("[ONBOARDING] Request received | user={}", username);
        return onboardingService.complete(username, moodSelection);
    }
}
