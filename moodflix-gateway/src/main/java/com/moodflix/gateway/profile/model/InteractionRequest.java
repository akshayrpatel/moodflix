package com.moodflix.gateway.profile.model;

public record InteractionRequest(String movieId, InteractionType interactionType, double weight) {
    public enum InteractionType {
        LIKE,
        DISLIKE,
        WATCHLIST,
        WATCHED,
        SKIP,
        REWATCH
    }
}
