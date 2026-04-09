package com.moodflix.gateway.risk.service.evaluator.anomaly;

import com.moodflix.gateway.risk.model.BehavioralAnomaly;
import com.moodflix.gateway.risk.model.RequestContext;
import com.moodflix.gateway.risk.model.SessionContext;

import java.util.Optional;

/**
 * Strategy interface for detecting a single type of anomaly
 * in an incoming request.
 *
 * <p>Each implementation detects one specific suspicious pattern —
 * impossible travel, user agent change, repeated anomalies, etc.
 * New anomaly types are added by implementing this interface and
 * registering as a Spring bean. No existing code changes.</p>
 *
 * <p>Implementations must be stateless and thread-safe — they are
 * singleton Spring beans called concurrently across many requests.</p>
 */
public interface AnomalyDetector {

    /**
     * Evaluates the incoming request against stored session state.
     *
     * <p>Returns an empty {@link Optional} if no anomaly is detected.
     * Returns a populated {@link BehavioralAnomaly} if a suspicious
     * pattern is found — more expressive than returning zero.</p>
     *
     * @param stored   the user's last known session state
     * @param incoming the current request being evaluated
     * @return an {@link Optional} containing the detected anomaly,
     *         or empty if the request looks clean
     */
    Optional<BehavioralAnomaly> evaluate(SessionContext stored, RequestContext incoming);

    /**
     * Short human-readable label identifying what this detector checks.
     * Used in audit logs and risk verdicts.
     *
     * @return a short reason string e.g. {@code "impossible_travel"}
     */
    String reason();
}