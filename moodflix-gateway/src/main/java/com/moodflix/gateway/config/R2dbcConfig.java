package com.moodflix.gateway.config;

import com.moodflix.gateway.auth.model.User;
import io.r2dbc.postgresql.codec.Vector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * R2DBC configuration for pgvector type conversion and entity callbacks.
 *
 * <p>The PostgreSQL R2DBC driver maps {@code vector(384)} columns to
 * {@link io.r2dbc.postgresql.codec.Vector}, but Spring Data R2DBC does
 * not know how to convert that to {@code float[]}. Custom converters
 * bridge the gap in both directions.</p>
 */
@Configuration
public class R2dbcConfig {

    /**
     * Registers custom converters for pgvector <-> float[] mapping.
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(
                PostgresDialect.INSTANCE,
                List.of(new VectorToFloatArrayConverter(), new FloatArrayToVectorConverter())
        );
    }

    /**
     * Reads: pgvector {@link Vector} -> {@code float[]}.
     */
    @ReadingConverter
    static class VectorToFloatArrayConverter implements Converter<Vector, float[]> {
        @Override
        public float[] convert(Vector source) {
            return source.getVector();
        }
    }

    /**
     * Writes: {@code float[]} -> pgvector {@link Vector}.
     */
    @WritingConverter
    static class FloatArrayToVectorConverter implements Converter<float[], Vector> {
        @Override
        public Vector convert(float[] source) {
            return Vector.of(source);
        }
    }

    /**
     * Sets {@code isNew = false} on every User loaded from the database.
     *
     * <p>Without this, the {@code @Transient isNew} field defaults to
     * {@code true} after every read, causing an INSERT (duplicate key)
     * instead of UPDATE on the next save.</p>
     */
    @Bean
    AfterConvertCallback<User> userAfterConvertCallback() {
        return (user, table) -> {
            user.setNew(false);
            return Mono.just(user);
        };
    }
}
