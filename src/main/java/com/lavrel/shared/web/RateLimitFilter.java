package com.lavrel.shared.web;

import com.lavrel.shared.error.ApiError;
import com.lavrel.shared.error.ErrorResponseWriter;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.domain.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protects the API from floods (free Render instance, free Supabase database). It runs after authentication, so it
 * knows who is calling:
 *  - anonymous callers: per client address, plus one shared limit for all of them together, so faking addresses
 *    cannot get around it;
 *  - signed-in users: per user;
 *  - account syncs (they call Steam a lot): a separate, small limit per user;
 *  - admins and the health check are not limited.
 * Over the limit: 429 too_many_requests with a Retry-After header (seconds). Built in SecurityConfig, not a bean, so
 * it does not also run as a plain servlet filter.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter anonymous;
    private final RateLimiter anonymousTotal;
    private final RateLimiter users;
    private final RateLimiter syncs;
    private final ErrorResponseWriter writer;

    public RateLimitFilter(RateLimiter anonymous, RateLimiter anonymousTotal, RateLimiter users, RateLimiter syncs,
                           ErrorResponseWriter writer) {
        this.anonymous = anonymous;
        this.anonymousTotal = anonymousTotal;
        this.users = users;
        this.syncs = syncs;
        this.writer = writer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long wait = waitSeconds(request);
        if (wait > 0) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(wait));
            writer.write(response, HttpStatus.TOO_MANY_REQUESTS, new ApiError("too_many_requests",
                    "Too many requests. Try again in " + wait + " seconds."));
            return;
        }
        chain.doFilter(request, response);
    }

    private long waitSeconds(HttpServletRequest request) {
        AuthenticatedUser user = currentUser();
        if (user == null) {
            long wait = anonymous.tryAcquire("ip:" + clientAddress(request));
            return wait > 0 ? wait : anonymousTotal.tryAcquire("all");
        }
        if (user.role() == Role.ADMIN && user.emailVerified()) {
            return 0;
        }
        if ("POST".equals(request.getMethod()) && request.getRequestURI().endsWith("/sync")) {
            long wait = syncs.tryAcquire("sync:" + user.userId());
            if (wait > 0) {
                return wait;
            }
        }
        return users.tryAcquire("user:" + user.userId());
    }

    private static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user ? user : null;
    }

    /**
     * Behind Render's proxy the caller's address is the first entry of X-Forwarded-For. A client can put anything
     * there, which only lets it share or dodge a per-address bucket; the shared anonymous limit still applies.
     */
    static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
