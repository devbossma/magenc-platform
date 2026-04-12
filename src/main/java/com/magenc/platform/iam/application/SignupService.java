package com.magenc.platform.iam.application;

import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.RawPassword;
import com.magenc.platform.tenancy.TenantContext;
import com.magenc.platform.tenancy.TenantProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles agency signup: provisions a new tenant schema and creates the
 * first OWNER user inside it.
 *
 * <p>This is the moment where the tenancy and IAM modules cooperate to
 * bring a new agency into existence. The full lifecycle is:
 *
 * <ol>
 *   <li>Insert {@code admin.agency} row with status PROVISIONING (TODO when
 *       agency catalog module exists; for now we provision the schema directly)</li>
 *   <li>Call {@link TenantProvisioningService#provisionSchema(String)} to
 *       create {@code tenant_<slug>} and run V1__baseline_tenant_schema.sql</li>
 *   <li>Switch the request's TenantContext to the new tenant</li>
 *   <li>Insert the OWNER user into the new tenant's {@code agency_user} table</li>
 *   <li>Issue a token pair so the user is logged in immediately</li>
 * </ol>
 *
 * <p>The whole flow runs in a single transaction. If anything fails — schema
 * creation, user insertion, token issuance — the transaction rolls back and
 * the agency does not exist. This is critical for avoiding orphan tenants.
 *
 * <p><b>Note for future:</b> when we add the agency catalog, this service
 * will also write to {@code admin.agency}. That insert needs to happen
 * BEFORE the tenant schema is created and BEFORE the TenantContext is switched,
 * because writes to admin.agency must use the admin schema.
 */

@Service
public class SignupService {

    private static final Logger log = LoggerFactory.getLogger(SignupService.class);

    private final TenantProvisioningService provisioningService;
    private final TenantUserCreationService userCreationService;

    public SignupService(
            TenantProvisioningService provisioningService,
            TenantUserCreationService userCreationService) {
        this.provisioningService = provisioningService;
        this.userCreationService = userCreationService;
    }

    public TokenService.TokenPair signup(SignupCommand command) {
        validateSlug(command.tenantSlug());

        // Transaction 1: provision schema (admin context, own transaction inside provisioningService)
        log.info("Provisioning new tenant schema: {}", command.tenantSlug());
        provisioningService.provisionSchema(command.tenantSlug());

        // Switch context BEFORE opening the next transaction so Hibernate
        // sees the new tenant when it borrows a connection
        TenantContext.set(command.tenantSlug());

        try {
            // Transaction 2: create user in tenant context (opens fresh Hibernate session)
            return userCreationService.createOwnerAndIssueTokens(command);
        } catch (Exception e) {
            // Cleanup orphan schema on failure
            log.error("User creation failed for tenant={}, rolling back schema",
                    command.tenantSlug(), e);
            try {
                provisioningService.dropSchema(command.tenantSlug());
            } catch (Exception cleanupError) {
                log.error("Failed to clean up orphan schema {}",
                        command.tenantSlug(), cleanupError);
            }
            throw e;
        }
    }

    private void validateSlug(String slug) {
        if (slug == null || !slug.matches("^[a-z0-9_]{3,63}$")) {
            throw new InvalidSlugException(
                    "Slug must be 3-63 lowercase alphanumeric characters or underscores");
        }
        if (slug.equals("admin") || slug.equals("api") || slug.equals("www")
                || slug.equals("app") || slug.equals("auth")) {
            throw new InvalidSlugException("Slug is reserved: " + slug);
        }
    }

    public record SignupCommand(
            String tenantSlug,
            String agencyDisplayName,
            Email email,
            RawPassword password,
            String displayName
    ) {}

    public static class InvalidSlugException extends RuntimeException {
        public InvalidSlugException(String message) {
            super(message);
        }
    }
}
