// Exists because resolving the tenant for a request is the single most security-critical operation: this file's correctness determines data isolation.
package com.magenc.platform.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class TenantResolutionFilter extends OncePerRequestFilter {

    // Exists because letting controllers set TenantContext themselves leaks the concern across the codebase and risks missed cleanup.
    // Paths listed here default to admin context when no tenant was resolvable from JWT or Host header.
    private static final Set<String> ADMIN_CONTEXT_PATHS = Set.of(
            "/v1/auth/signup",
            "/v1/auth/login",
            "/v1/auth/discover",
            "/v1/auth/refresh"
    );

    // Exists because Host-derived tenant slugs flow into SET search_path — we MUST validate before trusting.
    private static final Pattern VALID_SLUG = Pattern.compile("^[a-z0-9_]{3,63}$");

    private final String rootDomain;
    private final List<String> publicPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantResolutionFilter(TenancyProperties properties) {
        this.rootDomain = properties.rootDomain();
        this.publicPaths = properties.publicPaths();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        try {
            String tenantFromJwt = extractTenantFromJwt();
            String tenantFromHost = extractTenantFromHost(request.getServerName());
            String headerTenant = request.getHeader("X-Tenant-Slug");

            String resolvedTenant = null;

            if (tenantFromJwt != null) {
                resolvedTenant = tenantFromJwt;
                if (headerTenant != null && !headerTenant.equals(tenantFromJwt)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Tenant mismatch between JWT and X-Tenant-Slug header");
                    return;
                }
            } else if (tenantFromHost != null) {
                resolvedTenant = tenantFromHost;
            } else if (ADMIN_CONTEXT_PATHS.contains(request.getRequestURI())) {
                // Auth endpoints (signup, login, discover, refresh) run in admin context
                // because they need to read admin.platform_user and admin.agency before any tenant exists.
                resolvedTenant = "admin";
            }

            if (resolvedTenant != null) {
                TenantContext.set(resolvedTenant);
            } else if (!isPublicPath(request.getRequestURI())) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant could not be resolved");
                return;
            }

            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private @Nullable String extractTenantFromJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("tenant");
        }
        return null;
    }

    private @Nullable String extractTenantFromHost(String host) {
        if (host == null || host.isBlank()) return null;
        if (!host.endsWith("." + rootDomain) && !host.equals(rootDomain)) return null;
        if (host.equals(rootDomain)) return null;
        String prefix = host.substring(0, host.length() - rootDomain.length() - 1);
        int firstDot = prefix.indexOf('.');
        String slug = firstDot == -1 ? prefix : prefix.substring(0, firstDot);

        // Validate the extracted slug before returning: defense against SQL injection
        // if the slug ever reaches SET search_path via the connection provider.
        if (!VALID_SLUG.matcher(slug).matches()) return null;
        return slug;
    }

    private boolean isPublicPath(String uri) {
        for (String pattern : publicPaths) {
            if (pathMatcher.match(pattern, uri)) return true;
        }
        return false;
    }
}
