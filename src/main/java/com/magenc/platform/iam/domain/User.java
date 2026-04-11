package com.magenc.platform.iam.domain;

import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The User aggregate root.
 *
 * <p>This is a pure domain object: no JPA annotations, no Spring annotations,
 * no infrastructure dependencies. The infrastructure layer maps this to and
 * from a JPA entity, but those mapping concerns never leak in here.
 *
 * <p>State transitions happen through methods on this class (e.g.
 * {@link #recordLogin(Instant)}, {@link #suspend()}). Direct setters are
 * deliberately absent — invariants are enforced by behavior, not by the
 * absence of mutators.
 */
public final class User {

    private final UserId id;
    private final Email email;
    private HashedPassword password;
    private String displayName;
    private UserRole role;
    private UserStatus status;
    private final Instant createdAt;
    private @Nullable Instant lastLoginAt;

    /**
     * Reconstructs a User from persistence. Used only by repositories.
     */
    public static User reconstitute(
            UserId id,
            Email email,
            HashedPassword password,
            String displayName,
            UserRole role,
            UserStatus status,
            Instant createdAt,
            @Nullable Instant lastLoginAt) {
        return new User(id, email, password, displayName, role, status, createdAt, lastLoginAt);
    }

    /**
     * Creates a brand-new active user. Used during signup and invitations.
     */
    public static User create(
            Email email,
            HashedPassword password,
            String displayName,
            UserRole role) {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(role, "role");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be blank");
        }
        return new User(
                UserId.generate(),
                email,
                password,
                displayName.trim(),
                role,
                UserStatus.ACTIVE,
                Instant.now(),
                null);
    }

    private User(
            UserId id,
            Email email,
            HashedPassword password,
            String displayName,
            UserRole role,
            UserStatus status,
            Instant createdAt,
            @Nullable Instant lastLoginAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    // ----- behavior -----

    public boolean canSignIn() {
        return status == UserStatus.ACTIVE;
    }

    public void recordLogin(Instant when) {
        if (!canSignIn()) {
            throw new IllegalStateException("User cannot sign in: status is " + status);
        }
        this.lastLoginAt = when;
    }

    public void suspend() {
        if (status == UserStatus.SUSPENDED) {
            return;
        }
        this.status = UserStatus.SUSPENDED;
    }

    public void reactivate() {
        if (status == UserStatus.ACTIVE) {
            return;
        }
        this.status = UserStatus.ACTIVE;
    }

    public void changePassword(HashedPassword newPassword) {
        Objects.requireNonNull(newPassword, "newPassword");
        this.password = newPassword;
    }

    public void rename(String newDisplayName) {
        Objects.requireNonNull(newDisplayName, "newDisplayName");
        if (newDisplayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be blank");
        }
        this.displayName = newDisplayName.trim();
    }

    // ----- accessors -----

    public UserId id() { return id; }
    public Email email() { return email; }
    public HashedPassword password() { return password; }
    public String displayName() { return displayName; }
    public UserRole role() { return role; }
    public UserStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public @Nullable Instant lastLoginAt() { return lastLoginAt; }
}
