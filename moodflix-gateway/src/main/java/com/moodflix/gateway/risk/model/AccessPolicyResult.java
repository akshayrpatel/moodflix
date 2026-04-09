package com.moodflix.gateway.risk.model;

import java.util.List;

/**
 * The final access policy decision for the current request.
 *
 * <p>Produced by {@code RiskEvaluator} after running all anomaly
 * detectors. Carries both the decision ({@link Action}) and the
 * full reasoning ({@link BehavioralAnomaly} list) so filters can
 * act on the decision without knowing how it was reached.</p>
 *
 * <p>Replaces the previous {@code RiskVerdict} — adds explicit
 * action semantics and structured anomaly context instead of a
 * plain reason string.</p>
 *
 * @param riskLevel        the overall risk level of this request
 * @param action           what the gateway should do with this request
 * @param anomalies        the list of detected anomalies that led to
 *                         this decision — empty if request is clean
 * @param evaluationScore  the raw computed score before thresholding
 */
public record AccessPolicyResult(
        RiskLevel riskLevel,
        Action action,
        List<BehavioralAnomaly> anomalies,
        int evaluationScore
) {

    /**
     * The possible actions the gateway can take on a request.
     */
    public enum Action {
        /** Request is clean — pass through normally */
        ALLOW,

        /** Request is suspicious — require step-up authentication */
        CHALLENGE,

        /** Request is a severe threat — terminate immediately */
        BLOCK
    }

    /**
     * Returns true if this request should be blocked immediately.
     *
     * @return true if action is {@link Action#BLOCK}
     */
    public boolean isBlocked() {
        return action == Action.BLOCK;
    }

    /**
     * Returns true if this request requires step-up authentication.
     *
     * @return true if action is {@link Action#CHALLENGE}
     */
    public boolean requiresChallenge() {
        return action == Action.CHALLENGE;
    }
}