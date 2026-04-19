package com.magenc.platform.iam.infrastructure;


// Exists because credential stuffing attacks against /login and /signup are the #1 attack vector on any auth API:
// 10 attempts per IP per 15 minutes is the industry-standard baseline (Auth0, GitHub, etc).
// This is v1: in-memory per-instance. When we scale horizontally, replace with Redis-backed bucket4j.

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    // Auth endpoints that need rate limiting. Refresh is excluded because it's called by
    // legitimate clients on every token expiry and a compromised refresh token is already
    // limited by single-use semantics.
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/v1/auth/login",
            "/v1/auth/signup",
            "/v1/auth/discover"
    );

    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    // Exists because we need per-IP isolation but don't want unbounded memory growth:
    // ConcurrentHashMap with manual eviction on access keeps us lean without external deps.
    // TODO: replace with Redis-backed buckets when we run more than one instance.
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        if (!RATE_LIMITED_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={} path={}", clientIp, request.getRequestURI());
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(WINDOW.getSeconds()));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"RATE_LIMITED\",\"message\":\"Too many attempts. Try again in " +
                    WINDOW.toMinutes() + " minutes.\"}"
            );
        }
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_ATTEMPTS)
                .refillGreedy(MAX_ATTEMPTS, WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Behind a load balancer, X-Forwarded-For holds the real client IP.
        // TODO: only trust this header when request came from a known proxy IP range.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
