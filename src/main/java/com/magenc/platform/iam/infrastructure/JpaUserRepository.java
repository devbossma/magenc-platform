package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.User;
import com.magenc.platform.iam.domain.UserId;
import com.magenc.platform.iam.domain.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * JPA-backed implementation of the domain {@link UserRepository}.
 *
 * <p>This adapter is the only place where the domain {@code User} type
 * meets {@link UserEntity}. Conversion happens here, in both directions.
 */
@Component
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository delegate;

    public JpaUserRepository(SpringDataUserRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return delegate.findById(id.value()).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return delegate.findByEmail(email.value()).map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return delegate.existsByEmail(email.value());
    }

    @Override
    public User save(User user) {
        Optional<UserEntity> existing = delegate.findById(user.id().value());
        UserEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.updateFromDomain(user);
        } else {
            entity = UserEntity.fromDomain(user);
        }
        UserEntity saved = delegate.save(entity);
        return saved.toDomain();
    }

    @Override
    public long countOwners() {
        return delegate.countOwners();
    }
}
