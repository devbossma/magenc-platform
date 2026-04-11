package com.magenc.platform.iam.api;

import com.magenc.platform.iam.application.SignupService;
import com.magenc.platform.iam.application.TokenService;
import com.magenc.platform.iam.domain.AuthenticationFailedException;
import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.EmailAlreadyTakenException;
import com.magenc.platform.iam.domain.RawPassword;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain and application exceptions to HTTP responses.
 *
 * <p>Error responses are deliberately minimal — no stack traces, no
 * field-by-field validation details for auth errors (defeats user
 * enumeration). Validation errors on signup return field details
 * because there's no security harm in telling the user their password
 * was too short.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthFailed(AuthenticationFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(
                "INVALID_CREDENTIALS", "Invalid credentials"));
    }

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<Map<String, Object>> handleEmailTaken(EmailAlreadyTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(
                "EMAIL_TAKEN", "Email is already registered"));
    }

    @ExceptionHandler(RawPassword.WeakPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleWeakPassword(RawPassword.WeakPasswordException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(
                "WEAK_PASSWORD", e.getMessage()));
    }

    @ExceptionHandler(Email.InvalidEmailException.class)
    public ResponseEntity<Map<String, Object>> handleBadEmail(Email.InvalidEmailException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(
                "INVALID_EMAIL", e.getMessage()));
    }

    @ExceptionHandler(SignupService.InvalidSlugException.class)
    public ResponseEntity<Map<String, Object>> handleBadSlug(SignupService.InvalidSlugException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(
                "INVALID_SLUG", e.getMessage()));
    }

    @ExceptionHandler(TokenService.InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleBadToken(TokenService.InvalidTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(
                "INVALID_TOKEN", "Refresh token is invalid"));
    }

    private Map<String, Object> error(String code, String message) {
        return Map.of(
                "error", code,
                "message", message,
                "timestamp", Instant.now().toString()
        );
    }
}
