package com.moodflix.interactions.service.vector.provider;

import java.util.Optional;

/**
 * Contract for retrieving high-dimensional embedding vectors for media titles.
 * Decouples the vector calculation engine from vector store infrastructure (ChromaDB, REST, Cache).
 */
public interface MovieVectorProvider {

    /**
     * Retrieves the 384-dimensional semantic embedding for a movie.
     *
     * @param movieId unique identifier of the movie (e.g. TMDB ID)
     * @return Optional containing the float[384] vector, or empty if movie not found
     */
    Optional<float[]> getMovieVector(String movieId);
}