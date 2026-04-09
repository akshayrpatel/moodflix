package com.moodflix.gateway.risk.service.evaluator;

import com.moodflix.gateway.risk.model.AccessPolicyResult;
import com.moodflix.gateway.risk.model.RequestContext;
import com.moodflix.gateway.risk.model.SessionContext;

/**
 * Contract for evaluating the risk level of an incoming request
 * against a user's established session history.
 */
public interface RiskEvaluator {

    /**
     * Evaluates the risk level of the incoming request against
     * the user's stored session state.
     *
     * @param stored   the user's last known session state
     * @param incoming the current request being evaluated
     * @return an {@link AccessPolicyResult} containing the action,
     *         risk level, detected anomalies, and evaluation score
     */
    AccessPolicyResult evaluate(SessionContext stored, RequestContext incoming);
}