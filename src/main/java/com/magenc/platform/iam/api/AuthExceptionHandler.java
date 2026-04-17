// Exists because domain exceptions must map to HTTP status codes: this is the only place that translation happens.
package com.magenc.platform.iam.api;

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

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthFailed(AuthenticationFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("INVALID_CREDENTIALS", "Invalid credentials"));
    }

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<Map<String, Object>> handleEmailTaken(EmailAlreadyTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("EMAIL_TAKEN", "Email is already registered"));
    }

    @ExceptionHandler(RawPassword.WeakPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleWeakPw(RawPassword.WeakPasswordException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("WEAK_PASSWORD", e.getMessage()));
    }

    @ExceptionHandler(Email.InvalidEmailException.class)
    public ResponseEntity<Map<String, Object>> handleBadEmail(Email.InvalidEmailException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("INVALID_EMAIL", e.getMessage()));
    }

    @ExceptionHandler(TokenService.InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleBadToken(TokenService.InvalidTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("INVALID_TOKEN", "Token is invalid"));
    }

    private Map<String, Object> error(String code, String message) {
        return Map.of("error", code, "message", message, "timestamp", Instant.now().toString());
    }
}
