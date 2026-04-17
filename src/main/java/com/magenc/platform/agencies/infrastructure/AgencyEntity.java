// Exists because JPA needs its own entity: domain Agency stays free of persistence annotations.
package com.magenc.platform.agencies.infrastructure;

import com.magenc.platform.agencies.domain.Agency;
import com.magenc.platform.agencies.domain.AgencyId;
import com.magenc.platform.agencies.domain.AgencySlug;
import com.magenc.platform.agencies.domain.AgencyStatus;
import com.magenc.platform.agencies.domain.IsolationMode;
import com.magenc.platform.agencies.domain.PaymentStatus;
import com.magenc.platform.agencies.domain.PlanTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agency", schema = "admin")
public class AgencyEntity {

    @Id @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true, length = 63)
    private String slug;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING) @Column(name = "plan_tier", nullable = false, length = 32)
    private PlanTier planTier;

    @Enumerated(EnumType.STRING) @Column(name = "isolation_mode", nullable = false, length = 32)
    private IsolationMode isolationMode;

    @Column(name = "region", nullable = false, length = 32)
    private String region;

    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 32)
    private AgencyStatus status;

    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 32)
    private PaymentStatus paymentStatus;

    @Column(name = "trial_ends_at") private Instant trialEndsAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "activated_at") private Instant activatedAt;

    protected AgencyEntity() {}

    public static AgencyEntity fromDomain(Agency a) {
        var e = new AgencyEntity();
        e.id = a.id().value(); e.slug = a.slug().value(); e.displayName = a.displayName();
        e.planTier = a.planTier(); e.isolationMode = a.isolationMode(); e.region = a.region();
        e.status = a.status(); e.paymentStatus = a.paymentStatus();
        e.trialEndsAt = a.trialEndsAt(); e.createdAt = a.createdAt();
        e.updatedAt = Instant.now(); e.activatedAt = a.activatedAt();
        return e;
    }

    public Agency toDomain() {
        return Agency.reconstitute(
                AgencyId.of(id), AgencySlug.of(slug), displayName, planTier,
                isolationMode, region, status, paymentStatus, trialEndsAt,
                createdAt, activatedAt);
    }

    public void updateFromDomain(Agency a) {
        this.displayName = a.displayName(); this.planTier = a.planTier();
        this.isolationMode = a.isolationMode(); this.region = a.region();
        this.status = a.status(); this.paymentStatus = a.paymentStatus();
        this.trialEndsAt = a.trialEndsAt(); this.updatedAt = Instant.now();
        this.activatedAt = a.activatedAt();
    }

    public UUID getId() { return id; }
}
