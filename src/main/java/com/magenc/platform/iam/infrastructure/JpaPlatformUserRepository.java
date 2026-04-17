// Exists because the domain UserRepository interface must be implemented: this adapter converts between domain and JPA types.
package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.PlatformUser;
import com.magenc.platform.iam.domain.PlatformUserId;
import com.magenc.platform.iam.domain.PlatformUserRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaPlatformUserRepository implements PlatformUserRepository {
    private final SpringDataPlatformUserRepository delegate;

    public JpaPlatformUserRepository(SpringDataPlatformUserRepository delegate) {
        this.delegate = delegate;
    }

    @Override public Optional<PlatformUser> findById(PlatformUserId id) {
        return delegate.findById(id.value()).map(PlatformUserEntity::toDomain);
    }

    @Override public Optional<PlatformUser> findByEmail(Email email) {
        return delegate.findByEmail(email.value()).map(PlatformUserEntity::toDomain);
    }

    @Override public boolean existsByEmail(Email email) {
        return delegate.existsByEmail(email.value());
    }

    @Override public PlatformUser save(PlatformUser user) {
        Optional<PlatformUserEntity> existing = delegate.findById(user.id().value());
        PlatformUserEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.updateFromDomain(user);
        } else {
            entity = PlatformUserEntity.fromDomain(user);
        }
        return delegate.save(entity).toDomain();
    }
}
