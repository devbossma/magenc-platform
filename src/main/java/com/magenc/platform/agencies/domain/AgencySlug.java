// Exists because slugs have strict rules (lowercase, no spaces, no reserved words): validating at construction prevents bad data.
package com.magenc.platform.agencies.domain;

import java.util.Set;

public record AgencySlug(String value) {

    private static final Set<String> RESERVED = Set.of(
            "admin", "api", "www", "auth", "app", "public", "static",
            "assets", "cdn", "mail", "smtp", "ftp", "ssh", "root",
            "system", "platform", "support", "help", "status",
            "billing", "dashboard", "login", "signup", "register"
    );

    public AgencySlug {
        if (value == null || value.isBlank()) throw new InvalidSlugException("Slug cannot be blank");
        if (!value.matches("^[a-z0-9_]{3,63}$"))
            throw new InvalidSlugException("Slug must be 3-63 lowercase alphanumeric characters or underscores");
        if (RESERVED.contains(value))
            throw new ReservedSlugException("Slug is reserved: " + value);
    }

    public static AgencySlug of(String value) { return new AgencySlug(value); }
    @Override public String toString() { return value; }

    public static class InvalidSlugException extends RuntimeException {
        public InvalidSlugException(String msg) { super(msg); }
    }

    public static class ReservedSlugException extends RuntimeException {
        public ReservedSlugException(String msg) { super(msg); }
    }
}
