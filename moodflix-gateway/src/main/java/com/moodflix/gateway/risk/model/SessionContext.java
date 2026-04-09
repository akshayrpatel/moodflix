package com.moodflix.gateway.risk.model;

import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.Instant;

/**
 * Immutable snapshot of a user's session state stored in Redis.
 *
 * <p>Represents what the gateway knows about a user's current session —
 * their last known IP, user agent, risk score, and request history.
 * A new instance is created after each request reflecting the updated
 * state.</p>
 *
 * <p>Immutability is intentional — in a reactive pipeline, mutable
 * shared state across concurrent requests causes race conditions.
 * Each request works with its own snapshot.</p>
 */
@Getter
@Builder(toBuilder = true)
@JsonDeserialize(builder = SessionContext.SessionContextBuilder.class)
public final class SessionContext {

    private final String username;
    private final String lastKnownIp;
    private final String lastKnownUserAgent;
    private final Instant lastRequestTime;
    private final int riskScore;
    private final int consecutiveAnomalies;

    /**
     * Returns true if this session has no prior history.
     *
     * <p>A session is considered new when username is null —
     * meaning no previous request has been recorded for this user.
     * New sessions always produce a LOW risk verdict.</p>
     *
     * @return true if this is a first-time session
     */
    public boolean isNew() {
        return username == null;
    }

    /**
     * Creates an empty session with no prior history.
     *
     * <p>All fields default to null or zero.</p>
     *
     * @return a new empty {@link SessionContext}
     */
    public static SessionContext empty() {
        return SessionContext.builder().build();
    }

    /**
     * Required by Jackson for deserialization.
     *
     * <p>This class is immutable — no no-arg constructor or setters exist.
     * Jackson uses this builder instead of its default reflection strategy.
     * {@code withPrefix = ""} tells Jackson that builder methods are named
     * {@code username()} not {@code withUsername()}.</p>
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static final class SessionContextBuilder {}
}