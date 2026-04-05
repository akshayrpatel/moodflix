package com.moodflix.gateway.auth.model;

import com.moodflix.gateway.auth.service.UserService;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * Represents an authenticated user in the MoodFlix system.
 *
 * <p>This entity maps to the {@code users} table in PostgreSQL via R2DBC.
 * R2DBC is the reactive, non-blocking database driver — unlike JPA/JDBC
 * it does not block threads while waiting for database responses.</p>
 *
 * <p>Implements {@link Persistable} because R2DBC uses {@code isNew()}
 * to decide between INSERT and UPDATE. Since the UUID is generated
 * before save (not by the database), R2DBC would otherwise assume the
 * entity already exists and issue an UPDATE that matches zero rows.</p>
 *
 * <p>Note on taste_vector: pgvector's {@code vector(384)} type is not
 * natively supported by R2DBC. The field is mapped as {@code float[]}
 * and relies on PostgreSQL's implicit type coercion. Full pgvector
 * support requires a custom codec — tracked as future work.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table("users")
public class User implements Persistable<UUID> {

    /**
     * Primary key — UUID generated before insert, not by the database.
     * R2DBC does not support {@code @GeneratedValue} with UUID strategy.
     * {@link UserService} is responsible
     * for generating this value before saving.
     */
    @Id
    private UUID id;

    private String username;

    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("taste_vector")
    private float[] tasteVector;

    /**
     * Tracks whether this entity is new (not yet persisted).
     * Set to {@code true} by the builder, flipped to {@code false}
     * after the first save. Marked {@code @Transient} so R2DBC
     * does not try to map it to a database column.
     */
    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }
}