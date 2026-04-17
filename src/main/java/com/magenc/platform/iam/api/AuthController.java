// Exists because this is the ONLY HTTP surface for authentication: login, discover, switch, refresh, logout, me.
package com.magenc.platform.iam.api;

import com.magenc.platform.iam.api.dto.DiscoveryLoginRequest;
import com.magenc.platform.iam.api.dto.DiscoveryLoginResponse;
import com.magenc.platform.iam.api.dto.LoginRequest;
import com.magenc.platform.iam.api.dto.MeResponse;
import com.magenc.platform.iam.api.dto.RefreshRequest;
import com.magenc.platform.iam.api.dto.SwitchTenantRequest;
import com.magenc.platform.iam.api.dto.TokenResponse;
import com.magenc.platform.iam.application.AuthenticationService;
import com.magenc.platform.iam.application.SessionService;
import com.magenc.platform.iam.application.TokenService;
import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.RawPassword;
import com.magenc.platform.iam.domain.SessionId;
import com.magenc.platform.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthenticationService authService;
    private final TokenService tokenService;
    private final SessionService sessionService;

    public AuthController(AuthenticationService authService, TokenService tokenService,
                          SessionService sessionService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        TenantContext.set("admin");
        TokenService.TokenPair pair = authService.authenticateScoped(
                request.tenantSlug(),
                Email.of(request.email()),
                RawPassword.of(request.password()),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(toResponse(pair));
    }

    @PostMapping("/discover")
    public ResponseEntity<DiscoveryLoginResponse> discover(
            @Valid @RequestBody DiscoveryLoginRequest request,
            HttpServletRequest httpRequest) {
        TenantContext.set("admin");
        var result = authService.authenticateDiscovery(
                Email.of(request.email()),
                RawPassword.of(request.password()),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr());

        List<DiscoveryLoginResponse.AgencyInfo> agencies = result.memberships().stream()
                .map(m -> new DiscoveryLoginResponse.AgencyInfo(
                        m.slug(), m.displayName(), m.role(),
                        "https://" + m.slug() + ".orchestrate.marketing/login"))
                .toList();

        var userInfo = new DiscoveryLoginResponse.UserInfo(
                result.user().id().toString(),
                result.user().email().value(),
                result.user().displayName());

        // TODO: issue a short-lived discovery token for the /scope exchange
        String discoveryToken = "TODO_DISCOVERY_TOKEN";

        return ResponseEntity.ok(new DiscoveryLoginResponse(userInfo, agencies, discoveryToken));
    }

    @PostMapping("/switch")
    public ResponseEntity<TokenResponse> switchTenant(
            @Valid @RequestBody SwitchTenantRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        TenantContext.set("admin");
        TokenService.TokenPair pair = authService.authenticateScoped(
                request.targetTenantSlug(),
                Email.of(jwt.getClaimAsString("email")),
                // For switch, we trust the existing JWT instead of re-asking password.
                // This is a deliberate UX choice: switching workspaces shouldn't require
                // re-entering password if you have a valid session.
                // Security: the user already has a valid JWT, proving they authenticated.
                // We just need to verify they have membership in the target agency.
                // TODO: refactor authenticateScoped to accept either password or existing JWT
                RawPassword.of("SWITCH_NOT_IMPLEMENTED_YET"),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(toResponse(pair));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenService.TokenPair pair = tokenService.refresh(request.refreshToken());
        return ResponseEntity.ok(toResponse(pair));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        tokenService.revoke(request.refreshToken());
        if (jwt != null) {
            String sid = jwt.getClaimAsString("sid");
            if (sid != null) {
                sessionService.revokeSession(SessionId.fromString(sid), "USER_LOGOUT");
            }
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new MeResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("display_name"),
                jwt.getClaimAsString("tenant"),
                jwt.getClaimAsString("agency_id"),
                jwt.getClaimAsString("role"),
                jwt.getClaimAsString("sid")
        ));
    }

    private static TokenResponse toResponse(TokenService.TokenPair pair) {
        return TokenResponse.of(pair.accessToken(), pair.refreshToken(), pair.accessTokenTtl().toSeconds());
    }
}
