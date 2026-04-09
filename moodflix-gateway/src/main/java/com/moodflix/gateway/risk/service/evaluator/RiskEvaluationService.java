package com.moodflix.gateway.risk.service.evaluator;

import com.moodflix.gateway.risk.model.AccessPolicyResult;
import com.moodflix.gateway.risk.model.BehavioralAnomaly;
import com.moodflix.gateway.risk.model.RequestContext;
import com.moodflix.gateway.risk.model.RiskLevel;
import com.moodflix.gateway.risk.model.SessionContext;
import com.moodflix.gateway.risk.service.evaluator.anomaly.AnomalyDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link RiskEvaluator}.
 *
 * <p>Orchestrates a collection of {@link AnomalyDetector} strategies
 * to compute a risk assessment for each request. Each detector
 * contributes an independent anomaly — the final score is the sum
 * of all anomaly contributions applied on top of the time-decayed
 * stored score.</p>
 *
 * <p>Score to RiskLevel mapping:</p>
 * <ul>
 *   <li>0–39   → LOW    — clean request, allow</li>
 *   <li>40–69  → MEDIUM — suspicious, challenge</li>
 *   <li>70+    → HIGH   — severe threat, block</li>
 * </ul>
 *
 * <p>Anomaly to score contribution mapping:</p>
 * <ul>
 *   <li>HIGH anomaly   → 50 points</li>
 *   <li>MEDIUM anomaly → 20 points</li>
 *   <li>LOW anomaly    → 10 points</li>
 * </ul>
 *
 * <p>Time decay reduces the stored score by 5 points per 10 minutes
 * of inactivity — risk does not remain elevated indefinitely.</p>
 */
@Slf4j
@Service
public final class RiskEvaluationService implements RiskEvaluator {

    private static final int MEDIUM_THRESHOLD = 40;
    private static final int HIGH_THRESHOLD = 70;

    private static final int DECAY_POINTS_PER_STEP = 5;
    private static final int DECAY_INTERVAL_MINS = 10;

    private static final int HIGH_ANOMALY_SCORE    = 50;
    private static final int MEDIUM_ANOMALY_SCORE  = 20;
    private static final int LOW_ANOMALY_SCORE     = 10;

    private final List<AnomalyDetector> detectors;

    /**
     * @param detectors all registered anomaly detectors
     */
    public RiskEvaluationService(List<AnomalyDetector> detectors) {
        this.detectors = List.copyOf(detectors);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Evaluation steps:</p>
     * <ol>
     *   <li>Apply time decay to stored score</li>
     *   <li>Run all detectors, collect anomalies</li>
     *   <li>Sum anomaly score contributions</li>
     *   <li>Determine risk level and action from final score</li>
     * </ol>
     */
    @Override
    public AccessPolicyResult evaluate(SessionContext stored, RequestContext incoming) {
        int decayedScore = applyTimeDecay(stored, incoming);
        List<BehavioralAnomaly> anomalies = runDetectors(stored, incoming);

        int anomalyScore = calculateAnomalyScore(anomalies);
        int finalAnomalyScore = decayedScore + anomalyScore;

        RiskLevel riskLevel = determineRiskLevel(finalAnomalyScore);
        AccessPolicyResult.Action action = determineAction(riskLevel);

        log.debug("[RISK] user={} decayed={} anomalyScore={} final={} level={} action={}",
                incoming.username(), decayedScore, anomalyScore,
                finalAnomalyScore, riskLevel, action);

        return new AccessPolicyResult(riskLevel, action, anomalies, finalAnomalyScore);
    }

    /**
     * Reduces the stored risk score based on elapsed idle time.
     *
     * <p>Score drops by {@value DECAY_POINTS_PER_STEP} points per
     * {@value DECAY_INTERVAL_MINS} minutes of inactivity, floored at zero.
     * New sessions have no stored score — return zero immediately.</p>
     *
     * @param stored   the user's stored session state
     * @param incoming the current request
     * @return the decayed score, minimum zero
     */
    private int applyTimeDecay(SessionContext stored, RequestContext incoming) {
        if (stored.isNew() || stored.getLastRequestTime() == null) {
            return 0;
        }

        int minutesIdle = (int) Duration.between(
                stored.getLastRequestTime(),
                incoming.requestTime()
        ).toMinutes();
        int decay = (minutesIdle / DECAY_INTERVAL_MINS) * DECAY_POINTS_PER_STEP;

        return Math.max(0, stored.getRiskScore() - decay);
    }

    /**
     * Runs all detectors and collects detected anomalies.
     *
     * <p>Each detector is called exactly once — results are
     * reused for both score calculation and the result payload.</p>
     *
     * @param stored   the user's stored session state
     * @param incoming the current request
     * @return list of detected anomalies — empty if request is clean
     */
    private List<BehavioralAnomaly> runDetectors(SessionContext stored, RequestContext incoming) {
        return detectors.stream()
                .map(detector -> detector.evaluate(stored, incoming))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Calculates the total score contribution from all detected anomalies.
     *
     * <p>Score is derived from the severity of each anomaly —
     * no hardcoded scores live in individual detectors.</p>
     *
     * @param anomalies the detected anomalies
     * @return total score contribution
     */
    private int calculateAnomalyScore(List<BehavioralAnomaly> anomalies) {
        return anomalies.stream()
                .mapToInt(anomaly -> switch (anomaly.severity()) {
                    case HIGH   -> HIGH_ANOMALY_SCORE;
                    case MEDIUM -> MEDIUM_ANOMALY_SCORE;
                    case LOW    -> LOW_ANOMALY_SCORE;
                })
                .sum();
    }

    /**
     * Determines the risk level from the final computed score.
     *
     * @param score the final risk score
     * @return the corresponding {@link RiskLevel}
     */
    private RiskLevel determineRiskLevel(int score) {
        if (score >= HIGH_THRESHOLD)   return RiskLevel.HIGH;
        if (score >= MEDIUM_THRESHOLD) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    /**
     * Determines the gateway action from the risk level.
     *
     * <p>MEDIUM risk results in CHALLENGE — a step-up authentication
     * signal injected via headers for the frontend to act on.</p>
     *
     * @param riskLevel the determined risk level
     * @return the corresponding {@link AccessPolicyResult.Action}
     */
    private AccessPolicyResult.Action determineAction(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH   -> AccessPolicyResult.Action.BLOCK;
            case MEDIUM -> AccessPolicyResult.Action.CHALLENGE;
            case LOW    -> AccessPolicyResult.Action.ALLOW;
        };
    }

//    /**
//     * Builds a reason string from pre-computed detector results.
//     *
//     * @param results      the pre-computed detector results
//     * @param decayedScore non-zero means prior history is contributing
//     * @return a short reason string for audit logs
//     */
//    private String buildReason(List<DetectorResult> results, int decayedScore) {
//        String triggeredReasons = results.stream()
//                .filter(result -> result.score() > 0)
//                .map(DetectorResult::reason)
//                .reduce((a, b) -> a + "," + b)
//                .orElse("");
//
//        if (triggeredReasons.isEmpty() && decayedScore > 0) {
//            return "elevated_session_history";
//        }
//
//        return triggeredReasons.isEmpty() ? "clean" : triggeredReasons;
//    }
//
//    /**
//     * Holds the result of a single detector evaluation.
//     *
//     * @param reason the detector's reason string
//     * @param score  the score contribution — 0 if no anomaly detected
//     */
//    private record DetectorResult(String reason, int score) {}
}