package com.moodflix.gateway.risk.service.evaluator.anomaly;

import com.moodflix.gateway.risk.model.BehavioralAnomaly;
import com.moodflix.gateway.risk.model.RequestContext;
import com.moodflix.gateway.risk.model.RiskLevel;
import com.moodflix.gateway.risk.model.SessionContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Detects a pattern of repeated consecutive anomalies in a session.
 *
 * <p>Three or more consecutive anomalies without a clean request
 * in between suggests a persistent threat actor or ongoing session
 * compromise.</p>
 */
@Component
public final class RepeatedAnomalyDetector implements AnomalyDetector {

    private static final int ANOMALY_THRESHOLD = 3;

    @Override
    public Optional<BehavioralAnomaly> evaluate(SessionContext stored, RequestContext incoming) {

        if (stored.isNew()) {
            return Optional.empty();
        }

        if (stored.getConsecutiveAnomalies() < ANOMALY_THRESHOLD) {
            return Optional.empty();
        }

        String anomalyDescription = "User has " + stored.getConsecutiveAnomalies()
                + " consecutive anomalies in this session";

        return Optional.of(new BehavioralAnomaly(
                BehavioralAnomaly.AnomalyType.REPEATED_AUTH_FAILURE,
                RiskLevel.HIGH,
                anomalyDescription,
                Instant.now()
        ));
    }

    @Override
    public String reason() {
        return "repeated_anomaly_pattern";
    }
}