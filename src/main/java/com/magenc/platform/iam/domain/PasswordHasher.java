// Exists because the domain defines WHAT hashing means; the infrastructure decides HOW (bcrypt, argon2, etc).
package com.magenc.platform.iam.domain;

public interface PasswordHasher {
    HashedPassword hash(RawPassword raw);
    boolean matches(RawPassword raw, HashedPassword hashed);
}
