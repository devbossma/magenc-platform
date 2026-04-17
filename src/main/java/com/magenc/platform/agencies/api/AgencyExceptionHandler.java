// Exists because agency-specific domain exceptions map to HTTP status codes here, separate from auth exceptions.
package com.magenc.platform.agencies.api;

import com.magenc.platform.agencies.domain.AgencySlug;
import com.magenc.platform.agencies.domain.SlugAlreadyTakenException;
import com.magenc.platform.iam.domain.EmailAlreadyTakenException;
import com.magenc.platform.iam.domain.RawPassword;
import com.magenc.platform.iam.domain.Email;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.magenc.platform.agencies")
public class AgencyExceptionHandler {

    @ExceptionHandler(SlugAlreadyTakenException.class)
    public ResponseEntity<Map<String, Object>> handleSlugTaken(SlugAlreadyTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("SLUG_TAKEN", e.getMessage()));
    }

    @ExceptionHandler(AgencySlug.ReservedSlugException.class)
    public ResponseEntity<Map<String, Object>> handleReserved(AgencySlug.ReservedSlugException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("RESERVED_SLUG", e.getMessage()));
    }

    @ExceptionHandler(AgencySlug.InvalidSlugException.class)
    public ResponseEntity<Map<String, Object>> handleBadSlug(AgencySlug.InvalidSlugException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("INVALID_SLUG", e.getMessage()));
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

    private Map<String, Object> error(String code, String message) {
        return Map.of("error", code, "message", message, "timestamp", Instant.now().toString());
    }
}
