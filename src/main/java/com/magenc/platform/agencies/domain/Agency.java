// Exists because the agency is the tenant: this aggregate root owns the full lifecycle from provisioning to deletion.
package com.magenc.platform.agencies.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class Agency {

    private final AgencyId id;
    private final AgencySlug slug;
    private String displayName;
    private PlanTier planTier;
    private IsolationMode isolationMode;
    private String region;
    private AgencyStatus status;
    private PaymentStatus paymentStatus;
    private @Nullable Instant trialEndsAt;
    private final Instant createdAt;
    private @Nullable Instant activatedAt;

    public static Agency create(AgencySlug slug, String displayName, String region) {
        Objects.requireNonNull(slug);
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName required");
        return new Agency(
                AgencyId.generate(), slug, displayName.trim(),
                PlanTier.TRIAL, IsolationMode.SCHEMA, region,
                AgencyStatus.PROVISIONING, PaymentStatus.SIMULATED_PAID,
                Instant.now().plus(Duration.ofDays(30)), Instant.now(), null);
    }

    public static Agency reconstitute(
            AgencyId id, AgencySlug slug, String displayName, PlanTier planTier,
            IsolationMode isolationMode, String region, AgencyStatus status,
            PaymentStatus paymentStatus, @Nullable Instant trialEndsAt,
            Instant createdAt, @Nullable Instant activatedAt) {
        return new Agency(id, slug, displayName, planTier, isolationMode,
                region, status, paymentStatus, trialEndsAt, createdAt, activatedAt);
    }

    private Agency(AgencyId id, AgencySlug slug, String displayName, PlanTier planTier,
                   IsolationMode isolationMode, String region, AgencyStatus status,
                   PaymentStatus paymentStatus, @Nullable Instant trialEndsAt,
                   Instant createdAt, @Nullable Instant activatedAt) {
        this.id = id; this.slug = slug; this.displayName = displayName;
        this.planTier = planTier; this.isolationMode = isolationMode; this.region = region;
        this.status = status; this.paymentStatus = paymentStatus;
        this.trialEndsAt = trialEndsAt; this.createdAt = createdAt; this.activatedAt = activatedAt;
    }

    public void activate() {
        if (status != AgencyStatus.PROVISIONING)
            throw new IllegalStateException("Can only activate a PROVISIONING agency, current: " + status);
        this.status = AgencyStatus.ACTIVE;
        this.activatedAt = Instant.now();
    }

    public void suspend() { this.status = AgencyStatus.SUSPENDED; }

    public boolean hasValidPayment() {
        return switch (paymentStatus) {
            case SIMULATED_PAID, STRIPE_ACTIVE -> true;
            case NONE, STRIPE_PAST_DUE -> false;
        };
    }

    public boolean isActive() { return status == AgencyStatus.ACTIVE; }

    public AgencyId id() { return id; }
    public AgencySlug slug() { return slug; }
    public String displayName() { return displayName; }
    public PlanTier planTier() { return planTier; }
    public IsolationMode isolationMode() { return isolationMode; }
    public String region() { return region; }
    public AgencyStatus status() { return status; }
    public PaymentStatus paymentStatus() { return paymentStatus; }
    public @Nullable Instant trialEndsAt() { return trialEndsAt; }
    public Instant createdAt() { return createdAt; }
    public @Nullable Instant activatedAt() { return activatedAt; }
}
