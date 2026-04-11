package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.HashedPassword;
import com.magenc.platform.iam.domain.User;
import com.magenc.platform.iam.domain.UserId;
import com.magenc.platform.iam.domain.UserRole;
import com.magenc.platform.iam.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to {@code agency_user} in each tenant schema.
 *
 * <p>Note this is NOT the domain {@link User} type. The infrastructure layer
 * keeps a separate persistence model so domain code stays free of JPA
 * annotations. The {@link #toDomain()} and {@link #fromDomain(User)} methods
 * are the only conversion points.
 */
@Entity
@Table(name = "agency_user")
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Required by JPA. Do not call from application code. */
    protected UserEntity() {}

    private UserEntity(
            UUID id,
            String email,
            String passwordHash,
            String displayName,
            UserRole role,
            UserStatus status,
            Instant createdAt,
            Instant lastLoginAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserEntity fromDomain(User user) {
        return new UserEntity(
                user.id().value(),
                user.email().value(),
                user.password().value(),
                user.displayName(),
                user.role(),
                user.status(),
                user.createdAt(),
                user.lastLoginAt());
    }

    public User toDomain() {
        return User.reconstitute(
                UserId.of(id),
                Email.of(email),
                new HashedPassword(passwordHash),
                displayName,
                role,
                status,
                createdAt,
                lastLoginAt);
    }

    /**
     * Updates this entity's mutable fields from a domain object.
     * Used during {@code save()} of an existing user.
     */
    public void updateFromDomain(User user) {
        this.email = user.email().value();
        this.passwordHash = user.password().value();
        this.displayName = user.displayName();
        this.role = user.role();
        this.status = user.status();
        this.lastLoginAt = user.lastLoginAt();
    }

    public UUID getId() {
        return id;
    }
}
