// Exists because domain defines the contract for user persistence; infrastructure implements it with JPA.
package com.magenc.platform.iam.domain;

import java.util.Optional;

public interface PlatformUserRepository {
    Optional<PlatformUser> findById(PlatformUserId id);
    Optional<PlatformUser> findByEmail(Email email);
    boolean existsByEmail(Email email);
    PlatformUser save(PlatformUser user);
}
