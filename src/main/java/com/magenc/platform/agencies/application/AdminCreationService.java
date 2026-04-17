// Exists because Stage 1 of signup (user + agency + membership INSERT) must run in its own @Transactional boundary in admin context.
// It is a SEPARATE Spring bean because @Transactional on self-calls doesn't work with Spring's proxy model.
package com.magenc.platform.agencies.application;

import com.magenc.platform.agencies.domain.Agency;
import com.magenc.platform.agencies.domain.AgencyMembership;
import com.magenc.platform.agencies.domain.AgencyMembershipRepository;
import com.magenc.platform.agencies.domain.AgencyRepository;
import com.magenc.platform.agencies.domain.AgencySlug;
import com.magenc.platform.iam.domain.HashedPassword;
import com.magenc.platform.iam.domain.PasswordHasher;
import com.magenc.platform.iam.domain.PlatformUser;
import com.magenc.platform.iam.domain.PlatformUserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCreationService {

    private final PlatformUserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyMembershipRepository membershipRepository;
    private final PasswordHasher passwordHasher;

    public AdminCreationService(
            PlatformUserRepository userRepository,
            AgencyRepository agencyRepository,
            AgencyMembershipRepository membershipRepository,
            PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.agencyRepository = agencyRepository;
        this.membershipRepository = membershipRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Stage 1: creates platform_user + agency + agency_membership in a single admin-context transaction.
     * If any INSERT fails, the entire transaction rolls back. No orphans.
     */
    @Transactional
    public CreationResult createIdentityAndAgency(SignupCommand command) {
        // Create user
        HashedPassword hashed = passwordHasher.hash(command.password());
        PlatformUser user = PlatformUser.createWithPassword(command.email(), hashed, command.displayName());
        user.markEmailVerified(Instant.now()); // SIMULATED email verification
        user = userRepository.save(user);

        // Create agency
        Agency agency = Agency.create(
                AgencySlug.of(command.tenantSlug()),
                command.agencyDisplayName(),
                "local"); // TODO: derive region from request geolocation
        agency = agencyRepository.save(agency);

        // Create OWNER membership
        AgencyMembership membership = AgencyMembership.createOwner(
                user.id().value(), agency.id());
        membership = membershipRepository.save(membership);

        return new CreationResult(user, agency, membership);
    }

    public record CreationResult(PlatformUser user, Agency agency, AgencyMembership membership) {}
}
