package com.moodflix.gateway.auth.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents a verified user identity after successful JWT validation.
 *
 * <p>Created by {@code JwtAuthFilter} from validated JWT claims and
 * passed through the filter chain as a typed identity object.
 * This record is request-scoped — it is never persisted.</p>
 *
 * <p>Carrying {@code tokenIssuedAt} enables downstream filters to
 * enforce token age policies independent of expiry — for example,
 * forcing re-authentication after 23 hours even if the token has
 * not technically expired.</p>
 *
 * @param username       the verified username extracted from the JWT subject claim
 * @param tokenIssuedAt  when the token was originally issued — used for age checks
 * @param roles          the authorities granted to this user — empty until
 *                       role-based access control is fully implemented
 */
public record AuthenticatedPrincipal(
        String username,
        Instant tokenIssuedAt,
        List<String> roles
) {}