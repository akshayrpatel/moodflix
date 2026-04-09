package com.moodflix.gateway.risk.service.event;

import com.moodflix.gateway.risk.model.BehavioralAnomaly;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Logging-based implementation of {@link RiskEventEmitter}.
 */
@Slf4j
@Component
public final class LoggingRiskEventEmitter implements RiskEventEmitter {

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> emit(
            BehavioralAnomaly anomaly,
            String username,
            String endpoint) {

        log.warn("[ANOMALY] user={} type={} severity={} endpoint={} description={}",
                username,
                anomaly.type(),
                anomaly.severity(),
                endpoint,
                anomaly.description()
        );

        return Mono.empty();
    }
}