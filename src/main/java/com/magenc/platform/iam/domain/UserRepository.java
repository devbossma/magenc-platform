package com.magenc.platform.iam.domain;

import java.util.Optional;

/**
 * Repository interface for User aggregates.
 *
 * <p>This interface lives in the domain layer; the implementation lives in
 * infrastructure. Application services depend on this interface, never on
 * the JPA implementation. This is the dependency inversion principle in
 * its most literal form.
 *
 * <p>All operations are tenant-scoped automatically: they execute within
 * the current Hibernate session, which has been bound to the current
 * tenant's schema by the multi-tenancy machinery in the tenancy module.
 * Callers do not pass a tenant ID and cannot accidentally read another
 * tenant's data.
 */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    User save(User user);

    long countOwners();
}
