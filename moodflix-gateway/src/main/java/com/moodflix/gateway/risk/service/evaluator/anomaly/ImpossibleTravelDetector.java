package com.moodflix.gateway.risk.service.evaluator.anomaly;

import com.moodflix.gateway.risk.model.BehavioralAnomaly;
import com.moodflix.gateway.risk.model.RequestContext;
import com.moodflix.gateway.risk.model.RiskLevel;
import com.moodflix.gateway.risk.model.SessionContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Detects impossible travel — an IP address change within a time
 * window too short for legitimate geographic movement.
 *
 * <p>If a user's IP changes within 5 minutes of their last request,
 * it is physically implausible they moved to a new location.
 * This is a strong indicator of session hijacking or token theft.</p>
 */
@Component
public final class ImpossibleTravelDetector implements AnomalyDetector {

    private static final Duration TRAVEL_WINDOW = Duration.ofMinutes(5);

    @Override
    public Optional<BehavioralAnomaly> evaluate(SessionContext stored, RequestContext incoming) {
        if (stored.isNew() || stored.getLastKnownIp() == null) {
            return Optional.empty();
        }

        boolean ipChanged = !stored.getLastKnownIp().equals(incoming.ipAddress());
        if (!ipChanged) {
            return Optional.empty();
        }

        Duration timeSinceLastRequest = Duration.between(
                stored.getLastRequestTime(),
                incoming.requestTime()
        );
        if (timeSinceLastRequest.compareTo(TRAVEL_WINDOW) >= 0) {
            return Optional.empty();
        }

        String anomalyDescription = "IP changed from " + stored.getLastKnownIp()
                + " to " + incoming.ipAddress()
                + " within " + timeSinceLastRequest.toSeconds() + "s";

        return Optional.of(new BehavioralAnomaly(
                BehavioralAnomaly.AnomalyType.GEO_VELOCITY_VIOLATION,
                RiskLevel.HIGH,
                anomalyDescription,
                Instant.now()
        ));
    }

    @Override
    public String reason() {
        return "impossible_travel";
    }
}