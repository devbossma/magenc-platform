package com.magenc.platform.iam.application;

import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.EmailAlreadyTakenException;
import com.magenc.platform.iam.domain.HashedPassword;
import com.magenc.platform.iam.domain.PasswordHasher;
import com.magenc.platform.iam.domain.RawPassword;
import com.magenc.platform.iam.domain.User;
import com.magenc.platform.iam.domain.UserRepository;
import com.magenc.platform.iam.domain.UserRole;
import com.magenc.platform.tenancy.TenantContext;
import com.magenc.platform.tenancy.TenantProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public SignupService(
            TenantProvisioningService provisioningService,
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenService tokenService) {
        this.provisioningService = provisioningService;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    @Transactional
    public TokenService.TokenPair signup(SignupCommand command) {
        validateSlug(command.tenantSlug());

        // Step 1: provision the schema (runs admin-context Flyway migration)
        log.info("Provisioning new tenant schema: {}", command.tenantSlug());
        provisioningService.provisionSchema(command.tenantSlug());

        // Step 2: switch context to the new tenant for user creation
        TenantContext.set(command.tenantSlug());

        // Step 3: create the first OWNER user
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyTakenException(command.email());
        }
        HashedPassword hashed = passwordHasher.hash(command.password());
        User owner = User.create(
                command.email(),
                hashed,
                command.displayName(),
                UserRole.OWNER);
        owner = userRepository.save(owner);
        log.info("Created OWNER user={} for new tenant={}", owner.id(), command.tenantSlug());

        // Step 4: issue tokens so the new user is signed in
        return tokenService.issueTokens(owner, command.tenantSlug());
    }

    private void validateSlug(String slug) {
        if (slug == null || !slug.matches("^[a-z0-9_]{3,63}$")) {
            throw new InvalidSlugException(
                    "Slug must be 3-63 lowercase alphanumeric characters or underscores");
        }
        // Reserved slugs that conflict with our routing
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
