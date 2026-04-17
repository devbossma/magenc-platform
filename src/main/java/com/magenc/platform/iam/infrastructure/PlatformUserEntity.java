// Exists because JPA needs its own entity class: domain PlatformUser stays free of persistence annotations.
package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.HashedPassword;
import com.magenc.platform.iam.domain.PlatformUser;
import com.magenc.platform.iam.domain.PlatformUserId;
import com.magenc.platform.iam.domain.PlatformUserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_user", schema = "admin")
public class PlatformUserEntity {

    @Id @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 32)
    private PlatformUserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "sso_provider", length = 64)
    private String ssoProvider;

    @Column(name = "sso_subject", length = 255)
    private String ssoSubject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected PlatformUserEntity() {}

    public static PlatformUserEntity fromDomain(PlatformUser user) {
        var e = new PlatformUserEntity();
        e.id = user.id().value();
        e.email = user.email().value();
        e.passwordHash = user.password() != null ? user.password().value() : null;
        e.displayName = user.displayName();
        e.status = user.status();
        e.emailVerifiedAt = user.emailVerifiedAt();
        e.ssoProvider = user.ssoProvider();
        e.ssoSubject = user.ssoSubject();
        e.createdAt = user.createdAt();
        e.updatedAt = Instant.now();
        e.lastLoginAt = user.lastLoginAt();
        return e;
    }

    public PlatformUser toDomain() {
        return PlatformUser.reconstitute(
                PlatformUserId.of(id), Email.of(email),
                passwordHash != null ? new HashedPassword(passwordHash) : null,
                displayName, status, emailVerifiedAt, ssoProvider, ssoSubject,
                createdAt, lastLoginAt);
    }

    public void updateFromDomain(PlatformUser user) {
        this.email = user.email().value();
        this.passwordHash = user.password() != null ? user.password().value() : null;
        this.displayName = user.displayName();
        this.status = user.status();
        this.emailVerifiedAt = user.emailVerifiedAt();
        this.lastLoginAt = user.lastLoginAt();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
}
