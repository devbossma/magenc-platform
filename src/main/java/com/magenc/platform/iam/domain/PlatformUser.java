// Exists because user identity is a global concept: one person, many agency memberships, one password.
package com.magenc.platform.iam.domain;

import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class PlatformUser {

    private final PlatformUserId id;
    private final Email email;
    private @Nullable HashedPassword password;
    private String displayName;
    private PlatformUserStatus status;
    private @Nullable Instant emailVerifiedAt;
    private @Nullable String ssoProvider;
    private @Nullable String ssoSubject;
    private final Instant createdAt;
    private @Nullable Instant lastLoginAt;

    public static PlatformUser reconstitute(
            PlatformUserId id, Email email, @Nullable HashedPassword password,
            String displayName, PlatformUserStatus status,
            @Nullable Instant emailVerifiedAt, @Nullable String ssoProvider,
            @Nullable String ssoSubject, Instant createdAt, @Nullable Instant lastLoginAt) {
        return new PlatformUser(id, email, password, displayName, status,
                emailVerifiedAt, ssoProvider, ssoSubject, createdAt, lastLoginAt);
    }

    public static PlatformUser createWithPassword(Email email, HashedPassword password, String displayName) {
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName required");
        return new PlatformUser(
                PlatformUserId.generate(), email, password, displayName.trim(),
                PlatformUserStatus.ACTIVE, null, null, null, Instant.now(), null);
    }

    private PlatformUser(
            PlatformUserId id, Email email, @Nullable HashedPassword password,
            String displayName, PlatformUserStatus status,
            @Nullable Instant emailVerifiedAt, @Nullable String ssoProvider,
            @Nullable String ssoSubject, Instant createdAt, @Nullable Instant lastLoginAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.status = status;
        this.emailVerifiedAt = emailVerifiedAt;
        this.ssoProvider = ssoProvider;
        this.ssoSubject = ssoSubject;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public boolean canSignIn() {
        return status == PlatformUserStatus.ACTIVE;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void markEmailVerified(Instant when) {
        this.emailVerifiedAt = when;
    }

    public void recordLogin(Instant when) {
        if (!canSignIn()) throw new IllegalStateException("User cannot sign in: " + status);
        this.lastLoginAt = when;
    }

    public void suspend() { this.status = PlatformUserStatus.SUSPENDED; }
    public void reactivate() { this.status = PlatformUserStatus.ACTIVE; }

    public void changePassword(HashedPassword newPassword) {
        Objects.requireNonNull(newPassword);
        this.password = newPassword;
    }

    public PlatformUserId id() { return id; }
    public Email email() { return email; }
    public @Nullable HashedPassword password() { return password; }
    public String displayName() { return displayName; }
    public PlatformUserStatus status() { return status; }
    public @Nullable Instant emailVerifiedAt() { return emailVerifiedAt; }
    public @Nullable String ssoProvider() { return ssoProvider; }
    public @Nullable String ssoSubject() { return ssoSubject; }
    public Instant createdAt() { return createdAt; }
    public @Nullable Instant lastLoginAt() { return lastLoginAt; }
}
