package com.moodflix.gateway.onboarding.model;

import java.util.List;

/**
 * Mood-based onboarding selections from the user.
 *
 * <p>Replaces traditional movie rating onboarding — captures
 * emotional intent and viewing context instead of requiring
 * movie knowledge. Sent to FastAPI to compute the seed
 * taste vector.</p>
 *
 * @param mood        how the user is feeling tonight
 * @param company     who they are watching with
 * @param commitment  how deep they want to go
 * @param tiredOf     what they have had enough of lately
 */
public record MoodSelection(Mood mood, Company company, Commitment commitment, List<TiredOf> tiredOf) {
    public enum Mood {
        NEED_TO_FEEL,
        EDGE_OF_SEAT,
        JUST_LAUGH,
        MIND_BLOWN,
        QUIET_BEAUTIFUL,
        PURE_CHAOS,
        LEARN_SOMETHING
    }

    public enum Company {
        SOLO,
        DATE_NIGHT,
        FAMILY,
        FRIENDS
    }

    public enum Commitment {
        CASUAL,
        FOCUSED,
        COMFORT_REWATCH,
        EPIC
    }

    public enum TiredOf {
        SUPERHEROES,
        SEQUELS,
        HAPPY_ENDINGS,
        AMERICAN_SETTINGS,
        CGI_EVERYTHING,
        SLOW_BURNS
    }
}
