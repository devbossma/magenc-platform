// Exists because this is the signup orchestrator: the single place where pre-flight validation, identity creation, schema provisioning, and token issuance come together.
package com.magenc.platform.agencies.application;

import com.magenc.platform.agencies.domain.Agency;
import com.magenc.platform.agencies.domain.AgencyMembership;
import com.magenc.platform.agencies.domain.AgencyMembershipRepository;
import com.magenc.platform.agencies.domain.AgencyRepository;
import com.magenc.platform.agencies.domain.AgencySlug;
import com.magenc.platform.agencies.domain.SlugAlreadyTakenException;
import com.magenc.platform.iam.application.AuthEventLogger;
import com.magenc.platform.iam.application.SessionService;
import com.magenc.platform.iam.application.TokenService;
import com.magenc.platform.iam.domain.EmailAlreadyTakenException;
import com.magenc.platform.iam.domain.PlatformUser;
import com.magenc.platform.iam.domain.PlatformUserRepository;
import com.magenc.platform.iam.domain.Session;
import com.magenc.platform.tenancy.TenantContext;
import com.magenc.platform.tenancy.TenantProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgencyRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(AgencyRegistrationService.class);

    private final PlatformUserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyMembershipRepository membershipRepository;
    private final AdminCreationService adminCreationService;
    private final TenantProvisioningService provisioningService;
    private final TokenService tokenService;
    private final SessionService sessionService;
    private final AuthEventLogger authEventLogger;

    public AgencyRegistrationService(
            PlatformUserRepository userRepository,
            AgencyRepository agencyRepository,
            AgencyMembershipRepository membershipRepository,
            AdminCreationService adminCreationService,
            TenantProvisioningService provisioningService,
            TokenService tokenService,
            SessionService sessionService,
            AuthEventLogger authEventLogger) {
        this.userRepository = userRepository;
        this.agencyRepository = agencyRepository;
        this.membershipRepository = membershipRepository;
        this.adminCreationService = adminCreationService;
        this.provisioningService = provisioningService;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.authEventLogger = authEventLogger;
    }

    public SignupResult registerNewAgency(SignupCommand command, String userAgent, String ipAddress) {

        // ── STAGE 0: Pre-flight validation (no writes, no schema work) ──
        TenantContext.set("admin");

        AgencySlug slug = AgencySlug.of(command.tenantSlug()); // validates format + reserved

        if (agencyRepository.existsBySlug(slug)) {
            throw new SlugAlreadyTakenException(command.tenantSlug());
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyTakenException(command.email());
        }

        log.info("Pre-flight passed for signup: slug={}, email={}", command.tenantSlug(), command.email());

        // ── STAGE 1: Create identity + agency + membership (admin txn) ──
        AdminCreationService.CreationResult created = adminCreationService.createIdentityAndAgency(command);
        PlatformUser user = created.user();
        Agency agency = created.agency();
        AgencyMembership membership = created.membership();

        log.info("Stage 1 complete: user={}, agency={}, membership={}",
                user.id(), agency.id(), membership.id());

        // ── STAGE 2: Provision tenant schema ──
        try {
            provisioningService.provisionSchema(command.tenantSlug());

            // Activate the agency (back in admin context)
            TenantContext.set("admin");
            agency.activate();
            agencyRepository.save(agency);

            log.info("Stage 2 complete: schema tenant_{} provisioned and agency activated", command.tenantSlug());

        } catch (Exception e) {
            log.error("Stage 2 failed for tenant={}, rolling back", command.tenantSlug(), e);
            rollbackStage1(user, agency, membership, command.tenantSlug());
            authEventLogger.logFailure("SIGNUP", user.id().value(), agency.id().value(),
                    "SCHEMA_PROVISIONING_FAILED", ipAddress, userAgent);
            throw new RuntimeException("Agency registration failed during provisioning", e);
        }

        // ── STAGE 3: Issue tokens ──
        TenantContext.set("admin");

        Session session = sessionService.createSession(
                user.id(), agency.id().value(), membership.id(), userAgent, ipAddress);

        TokenService.TokenPair tokens = tokenService.issueTokens(
                user, command.tenantSlug(), agency.id().value(),
                membership.id(), membership.role().name(), session.id());

        authEventLogger.logSuccess("SIGNUP", user.id().value(), agency.id().value(),
                session.id().value(), ipAddress, userAgent);

        log.info("Signup complete for agency={} user={}", command.tenantSlug(), user.id());

        return new SignupResult(tokens, command.tenantSlug(), command.agencyDisplayName(), user.id().toString());
    }

    private void rollbackStage1(PlatformUser user, Agency agency, AgencyMembership membership, String slug) {
        try { provisioningService.dropSchema(slug); } catch (Exception ex) { log.error("Cleanup: failed to drop schema {}", slug, ex); }
        try { membershipRepository.deleteById(membership.id()); } catch (Exception ex) { log.error("Cleanup: failed to delete membership {}", membership.id(), ex); }
        try { agencyRepository.deleteById(agency.id()); } catch (Exception ex) { log.error("Cleanup: failed to delete agency {}", agency.id(), ex); }
        try { userRepository.findById(user.id()).ifPresent(u -> { /* TODO: delete user if no other memberships */ }); } catch (Exception ex) { log.error("Cleanup: failed for user {}", user.id(), ex); }
    }
}
