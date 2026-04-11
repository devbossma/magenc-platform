package com.magenc.platform.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the current tenant for the duration of a request.
 *
 * <p>Resolution priority:
 * <ol>
 *   <li><b>JWT claim</b> (preferred). If a valid JWT is present in the
 *       SecurityContext, the {@code tenant} claim is the source of truth.
 *       This is what authenticated requests use.</li>
 *   <li><b>Host header</b> (fallback). For unauthenticated requests
 *       (signup, login, public endpoints) the tenant comes from the
 *       subdomain of the Host header. The login endpoint then validates
 *       the user against this tenant before issuing a JWT.</li>
 *   <li><b>Cross-check</b>. If both a JWT claim AND an X-Tenant-Slug
 *       header are present, they must match. A mismatch is a 403
 *       (signal of token theft or replay).</li>
 * </ol>
 *
 * <p>This filter MUST run AFTER Spring Security's JWT validation filter
 * so the SecurityContext is populated. Spring Security's filters run at
 * order ~100 by default; we run at HIGHEST_PRECEDENCE+200 to be safely
 * after them.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final String rootDomain;
    private final List<String> publicPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantResolutionFilter(
            @Value("${magenc.tenancy.root-domain}") String rootDomain,
            @Value("${magenc.tenancy.public-paths}") List<String> publicPaths) {
        this.rootDomain = rootDomain;
        this.publicPaths = publicPaths;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        try {
            String tenantFromJwt = extractTenantFromJwt();
            String tenantFromHost = extractTenantFromHost(request.getServerName());
            String headerTenant = request.getHeader("X-Tenant-Slug");

            String resolvedTenant = null;

            if (tenantFromJwt != null) {
                // Authenticated request: JWT claim is authoritative
                resolvedTenant = tenantFromJwt;

                // Cross-check with X-Tenant-Slug header if present
                if (headerTenant != null && !headerTenant.equals(tenantFromJwt)) {
                    response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Tenant mismatch between JWT and X-Tenant-Slug header");
                    return;
                }
            } else if (tenantFromHost != null) {
                // Unauthenticated request: fall back to Host header
                resolvedTenant = tenantFromHost;
            }

            if (resolvedTenant != null) {
                TenantContext.set(resolvedTenant);
            } else if (!isPublicPath(request.getRequestURI())) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Tenant could not be resolved");
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
        if (host == null || host.isBlank()) {
            return null;
        }
        if (!host.endsWith("." + rootDomain) && !host.equals(rootDomain)) {
            return null;
        }
        if (host.equals(rootDomain)) {
            return null;
        }
        String prefix = host.substring(0, host.length() - rootDomain.length() - 1);
        int firstDot = prefix.indexOf('.');
        return firstDot == -1 ? prefix : prefix.substring(0, firstDot);
    }

    private boolean isPublicPath(String uri) {
        for (String pattern : publicPaths) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}
