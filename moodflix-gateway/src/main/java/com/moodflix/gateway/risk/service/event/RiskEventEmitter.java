package com.moodflix.gateway.risk.service.event;

import com.moodflix.gateway.risk.model.BehavioralAnomaly;
import reactor.core.publisher.Mono;

/**
 * Contract for emitting detected behavioural anomalies when
 * suspicious patterns are observed across multiple requests.
 *
 * <p>Decouples detection logic from emission logic — the current
 * implementation logs anomalies as structured events. Future
 * implementations can publish to Kafka or a remote SIEM without
 * touching any detection code.</p>
 *
 * <p>Implementations must be non-blocking and thread-safe.</p>
 */
public interface RiskEventEmitter {

    /**
     * Emits a detected behavioural anomaly.
     *
     * @param anomaly   the detected anomaly to emit
     * @param username  the user who triggered this anomaly
     * @param endpoint  the endpoint being accessed
     * @return a {@link Mono} that completes when emission finishes
     */
    Mono<Void> emit(BehavioralAnomaly anomaly, String username, String endpoint);
}