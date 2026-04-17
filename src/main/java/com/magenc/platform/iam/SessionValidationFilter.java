// Exists because JWT expiry alone cannot revoke compromised sessions: we check admin.session on every authenticated request.
package com.magenc.platform.iam;

import com.magenc.platform.iam.application.SessionService;
import com.magenc.platform.iam.domain.SessionId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 150)
public class SessionValidationFilter extends OncePerRequestFilter {
    private final SessionService sessionService;

    public SessionValidationFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String sid = jwtAuth.getToken().getClaimAsString("sid");
            if (sid != null) {
                try {
                    SessionId sessionId = SessionId.fromString(sid);
                    if (!sessionService.isSessionValid(sessionId)) {
                        SecurityContextHolder.clearContext();
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session has been revoked");
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    // malformed sid claim, reject
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid session");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
