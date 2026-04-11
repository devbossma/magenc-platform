package com.magenc.platform.iam.api;

import com.magenc.platform.iam.api.dto.LoginRequest;
import com.magenc.platform.iam.api.dto.MeResponse;
import com.magenc.platform.iam.api.dto.RefreshRequest;
import com.magenc.platform.iam.api.dto.SignupRequest;
import com.magenc.platform.iam.api.dto.TokenResponse;
import com.magenc.platform.iam.application.AuthenticationService;
import com.magenc.platform.iam.application.SignupService;
import com.magenc.platform.iam.application.TokenService;
import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.RawPassword;
import com.magenc.platform.tenancy.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. All return JSON, no HTML, no forms.
 *
 * <p>The frontend (Next.js) wraps these endpoints with route handlers
 * that store the returned tokens in httpOnly cookies. From the browser's
 * perspective, auth state lives in cookies; from this backend's perspective,
 * it lives in JWTs in the Authorization header. The cookie is just transport.
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final SignupService signupService;
    private final AuthenticationService authenticationService;
    private final TokenService tokenService;

    public AuthController(
            SignupService signupService,
            AuthenticationService authenticationService,
            TokenService tokenService) {
        this.signupService = signupService;
        this.authenticationService = authenticationService;
        this.tokenService = tokenService;
    }

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        var command = new SignupService.SignupCommand(
                request.tenantSlug(),
                request.agencyDisplayName(),
                Email.of(request.email()),
                RawPassword.of(request.password()),
                request.displayName());
        TokenService.TokenPair pair = signupService.signup(command);
        return ResponseEntity.ok(toResponse(pair));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        // Login is special: the request is unauthenticated, so we set the
        // tenant context manually from the request body before calling the
        // authentication service. The TenantResolutionFilter has already
        // tried to resolve from Host header (which it may have done in dev),
        // but the request body is the explicit signal we trust here.
        TenantContext.set(request.tenantSlug());
        TokenService.TokenPair pair = authenticationService.authenticate(
                Email.of(request.email()),
                RawPassword.of(request.password()));
        return ResponseEntity.ok(toResponse(pair));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenService.TokenPair pair = tokenService.refresh(request.refreshToken());
        return ResponseEntity.ok(toResponse(pair));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        tokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new MeResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("tenant"),
                jwt.getClaimAsString("role")
        ));
    }

    private static TokenResponse toResponse(TokenService.TokenPair pair) {
        return TokenResponse.of(
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessTokenTtl().toSeconds());
    }
}
