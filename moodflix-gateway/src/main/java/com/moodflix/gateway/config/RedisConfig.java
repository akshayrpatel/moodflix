package com.moodflix.gateway.config;

import com.moodflix.gateway.risk.model.SessionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Central Redis configuration for the MoodFlix gateway.
 *
 * <ul>
 *   <li>{@code sessionContextRedisTemplate} — stores {@link SessionContext}
 *       JSON objects for risk evaluation</li>
 *   <li>{@code vectorRedisTemplate} — stores taste vectors as JSON
 *       float arrays for fast retrieval</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    /**
     * Typed template for storing {@link SessionContext} objects.
     */
    @Bean
    public ReactiveRedisTemplate<String, SessionContext> sessionContextRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        var mapper = JsonMapper.builder().build();
        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new JacksonJsonRedisSerializer<>(mapper, SessionContext.class);
        var context = RedisSerializationContext.<String, SessionContext>newSerializationContext(keySerializer)
                        .value(valueSerializer)
                        .hashKey(keySerializer)
                        .hashValue(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    /**
     * Typed template for caching taste vectors as float arrays.
     */
    @Bean
    public ReactiveRedisTemplate<String, float[]> vectorRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        var mapper = JsonMapper.builder().build();
        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new JacksonJsonRedisSerializer<>(mapper, float[].class);
        var context = RedisSerializationContext.<String, float[]>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
