package com.moodflix.interactions.service.vector.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Retrieves pre-computed 384-dimensional movie embeddings by querying
 * the internal endpoint on moodflix-ai-engine.
 */
@Slf4j
@Component
@Primary
public class AiEngineMovieVectorProvider implements MovieVectorProvider {

    private final RestClient restClient;

    public AiEngineMovieVectorProvider(
            @Value("${ai.engine.url}") String aiEngineUrl,
            @Value("${ai.engine.timeout.connect:2000ms}") Duration connectTimeout,
            @Value("${ai.engine.timeout.read:3000ms}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(aiEngineUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public record MovieEmbeddingResponse(String movieId, List<Float> embedding) {}

    @Override
    public Optional<float[]> getMovieVector(String movieId) {
        try {
            MovieEmbeddingResponse response = restClient.get()
                    .uri("/api/internal/movies/{movieId}/embedding", movieId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("Movie {} not found in AI engine ChromaDB (404)", movieId);
                    })
                    .body(MovieEmbeddingResponse.class);

            if (response == null || response.embedding() == null) {
                return Optional.empty();
            }

            List<Float> list = response.embedding();
            float[] vector = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                vector[i] = list.get(i);
            }
            return Optional.of(vector);

        } catch (Exception ex) {
            log.error("Failed to fetch embedding for movieId: {} from AI Engine", movieId, ex);
            return Optional.empty();
        }
    }
}