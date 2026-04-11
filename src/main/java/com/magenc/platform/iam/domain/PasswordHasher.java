package com.magenc.platform.iam.domain;

import java.util.function.Function;

/**
 * Hashes raw passwords and verifies them against stored hashes.
 *
 * <p>This is a domain interface, not a Spring component. The implementation
 * lives in the infrastructure layer.
 *
 * <p><b>Design note:</b> the methods take a {@link Function} callback rather
 * than the raw password value directly. This is so {@link RawPassword} can
 * keep its raw value package-private and only expose it briefly to the
 * hasher via a closure. The raw password value never crosses the
 * domain/infrastructure boundary as a String.
 */
public interface PasswordHasher {

    HashedPassword hash(RawPassword raw);

    boolean matches(RawPassword raw, HashedPassword hashed);
}
