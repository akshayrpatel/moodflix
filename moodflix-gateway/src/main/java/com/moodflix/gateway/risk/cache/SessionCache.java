package com.moodflix.gateway.risk.cache;

import com.moodflix.gateway.risk.model.SessionContext;
import reactor.core.publisher.Mono;

/**
 * Contract for loading and persisting user session state.
 */
public interface SessionCache {

    /**
     * Loads the session context for the given username.
     *
     * <p>Returns {@link SessionContext#empty()} if no session exists
     * yet — never returns an empty Mono. Callers can always expect
     * a value.</p>
     *
     * @param username the authenticated user to load session for
     * @return a {@link Mono} emitting the stored or empty session
     */
    Mono<SessionContext> load(String username);

    /**
     * Persists an updated session context for the given username.
     *
     * <p>Refreshes the TTL on every save — the session expires only
     * after 24 hours of complete inactivity.</p>
     *
     * <p>Storage failures are logged and swallowed — a Redis write
     * failure must never fail the user's request.</p>
     *
     * @param username the authenticated user to save session for
     * @param context  the updated session state to persist
     * @return a {@link Mono} that completes when the save finishes
     */
    Mono<Void> save(String username, SessionContext context);
}