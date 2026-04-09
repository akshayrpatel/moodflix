package com.moodflix.gateway.risk.model;

import java.time.Instant;

/**
 * Represents a single behavioural anomaly detected in a user's
 * session — a pattern that deviates from their established baseline.
 *
 * <p>Anomalies are detected per-request by {@code AnomalyDetector}
 * implementations and carried as context inside
 * {@link AccessPolicyResult} so filters and audit logs have full
 * visibility into what triggered a risk decision.</p>
 *
 * @param type        the category of anomaly detected
 * @param severity    how dangerous this specific anomaly is
 * @param description human-readable explanation of what was detected
 * @param detectedAt  when this anomaly was observed
 */
public record BehavioralAnomaly(
        AnomalyType type,
        RiskLevel severity,
        String description,
        Instant detectedAt
) {
    /**
     * Categories of behavioural anomalies this system detects.
     */
    public enum AnomalyType {
        GEO_VELOCITY_VIOLATION,
        DEVICE_FINGERPRINT_MISMATCH,
        SOFT_IP_CHANGE,
        REPEATED_AUTH_FAILURE,
        BURST_REQUESTS,
        REPEATED_FORBIDDEN
    }
}