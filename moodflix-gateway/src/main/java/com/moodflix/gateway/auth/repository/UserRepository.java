package com.moodflix.gateway.auth.repository;

import com.moodflix.gateway.auth.model.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for {@link User} persistence.
 *
 * <p>Extends {@link ReactiveCrudRepository} which provides non-blocking
 * database operations returning {@link Mono}.
 * All methods return reactive types - no thread blocking occurs.</p>
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email to search for
     * @return a {@link Mono} emitting the user if found, empty if not
     */
    Mono<User> findByEmail(String email);

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return a {@link Mono} emitting the user if found, empty if not
     */
    Mono<User> findByUsername(String username);
}