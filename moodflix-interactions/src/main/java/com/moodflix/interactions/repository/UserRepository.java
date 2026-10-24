package com.moodflix.interactions.repository;

import com.moodflix.interactions.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for managing persistent {@link User} profiles.
 * <p>
 * Inherits CRUD operations from {@link JpaRepository}. All update queries automatically
 * execute within Spring's transaction boundary and enforce Hibernate {@code @Version}
 * optimistic locking checks.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Inherits findById(UUID) and save(User) with built-in optimistic locking
}