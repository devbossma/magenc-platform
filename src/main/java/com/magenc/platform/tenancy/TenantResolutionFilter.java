package com.magenc.platform.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Resolves the tenant from the request's subdomain and populates
 * {@link TenantContext} for the duration of the request.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code acme.magenc.com} resolves to tenant "acme"</li>
 *   <li>{@code beta.magenc.local} resolves to tenant "beta"</li>
 *   <li>{@code magenc.com} - no tenant (rejected unless public path)</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final String rootDomain;
    private final List<String> publicPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantResolutionFilter(
            TenancyProperties properties) {
        this.rootDomain = properties.rootDomain();
        this.publicPaths = properties.publicPaths();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String host = request.getServerName();
        String tenantId = extractTenantFromHost(host);
        boolean isPublicPath = isPublicPath(request.getRequestURI());

        try {
            if (tenantId != null) {
                TenantContext.set(tenantId);
            } else if (!isPublicPath) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Tenant subdomain required");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
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
