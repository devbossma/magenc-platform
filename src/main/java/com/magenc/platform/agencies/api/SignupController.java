// Exists because signup is an agency lifecycle event, not an auth event: it lives in the agencies module, not iam.
// Tenant context is set to admin by TenantResolutionFilter for /v1/auth/signup.
package com.magenc.platform.agencies.api;

import com.magenc.platform.agencies.api.dto.SignupRequest;
import com.magenc.platform.agencies.api.dto.SignupResponse;
import com.magenc.platform.agencies.application.AgencyRegistrationService;
import com.magenc.platform.agencies.application.SignupCommand;
import com.magenc.platform.agencies.application.SignupResult;
import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.RawPassword;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class SignupController {

    private final AgencyRegistrationService registrationService;

    public SignupController(AgencyRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request,
                                                 HttpServletRequest httpRequest) {
        SignupCommand command = new SignupCommand(
                request.tenantSlug(),
                request.agencyDisplayName(),
                Email.of(request.email()),
                RawPassword.of(request.password()),
                request.displayName());

        SignupResult result = registrationService.registerNewAgency(
                command,
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(new SignupResponse(
                result.tokens().accessToken(),
                result.tokens().refreshToken(),
                result.tokens().accessTokenTtl().toSeconds(),
                "Bearer",
                result.agencySlug(),
                result.agencyDisplayName(),
                result.userId()));
    }
}
