package com.moodflix.gateway.risk.service.evaluator.anomaly;

import com.moodflix.gateway.risk.model.BehavioralAnomaly;
import com.moodflix.gateway.risk.model.RequestContext;
import com.moodflix.gateway.risk.model.RiskLevel;
import com.moodflix.gateway.risk.model.SessionContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Detects a mid-session user agent change.
 *
 * <p>A user agent change during an active session indicates the
 * request is coming from a different browser or device than the
 * one that authenticated. Strong indicator of session token theft.</p>
 */
@Component
public final class UserAgentChangeDetector implements AnomalyDetector {

    @Override
    public Optional<BehavioralAnomaly> evaluate(SessionContext stored, RequestContext incoming) {

        if (stored.isNew() || stored.getLastKnownUserAgent() == null) {
            return Optional.empty();
        }

        boolean userAgentChanged = !stored.getLastKnownUserAgent()
                .equals(incoming.userAgent());

        if (!userAgentChanged) {
            return Optional.empty();
        }

        String anomalyDescription = "User agent changed from '"
                + stored.getLastKnownUserAgent()
                + "' to '" + incoming.userAgent() + "'";

        return Optional.of(new BehavioralAnomaly(
                BehavioralAnomaly.AnomalyType.DEVICE_FINGERPRINT_MISMATCH,
                RiskLevel.MEDIUM,
                anomalyDescription,
                Instant.now()
        ));
    }

    @Override
    public String reason() {
        return "user_agent_change";
    }
}