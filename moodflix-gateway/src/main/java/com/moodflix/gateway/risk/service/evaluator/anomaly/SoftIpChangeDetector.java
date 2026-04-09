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
 * Detects a soft IP change — an IP address change after the
 * impossible travel window has passed.
 *
 * <p>Could be legitimate — a user switching from office WiFi to
 * home network. Worth flagging but not alarming.</p>
 */
@Component
public final class SoftIpChangeDetector implements AnomalyDetector {

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

        if (timeSinceLastRequest.compareTo(TRAVEL_WINDOW) < 0) {
            return Optional.empty();
        }

        String anomalyDescription = "IP changed from " + stored.getLastKnownIp()
                + " to " + incoming.ipAddress()
                + " after " + timeSinceLastRequest.toMinutes() + " minutes";

        return Optional.of(new BehavioralAnomaly(
                BehavioralAnomaly.AnomalyType.SOFT_IP_CHANGE,
                RiskLevel.MEDIUM,
                anomalyDescription,
                Instant.now()
        ));
    }

    @Override
    public String reason() {
        return "soft_ip_change";
    }
}