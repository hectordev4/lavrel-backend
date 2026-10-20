package com.lavrel.shared.security;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.domain.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer <Firebase ID token>", asks the user feature to authenticate it
 * (find or create the user) and stores the resulting AuthenticatedUser as the principal.
 * Users whose Firebase UID is in app.admin.firebase-uids also get ROLE_ADMIN (for /api/admin/**).
 * Controllers receive the principal with @AuthenticationPrincipal and only ever see IDs, never a token.
 *
 * Deliberately NOT a @Component: a Filter bean would also be registered in the servlet container
 * and run outside the security chain. SecurityConfig creates it and adds it to the chain.
 *
 * The client always gets the same generic 401; the real reason is written to the server log
 * (never the token itself) so a rejected token can be diagnosed.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticateUserUseCase authenticateUser;
    private final AuthenticationEntryPoint entryPoint;
    private final Set<String> adminFirebaseUids;

    public FirebaseAuthenticationFilter(AuthenticateUserUseCase authenticateUser,
                                        AuthenticationEntryPoint entryPoint,
                                        Set<String> adminFirebaseUids) {
        this.authenticateUser = authenticateUser;
        this.entryPoint = entryPoint;
        this.adminFirebaseUids = adminFirebaseUids;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null) {
            logger.debug("No Authorization header on " + request.getMethod() + " " + request.getRequestURI());
        } else if (!header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            logger.warn("Authorization header is present but does not start with 'Bearer '");
        } else {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                AuthenticatedUser user = authenticateUser.authenticate(token);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                        adminFirebaseUids.contains(user.firebaseUid())
                                ? AuthorityUtils.createAuthorityList("ROLE_ADMIN")
                                : AuthorityUtils.NO_AUTHORITIES));
                SecurityContextHolder.setContext(context);
            } catch (InvalidTokenException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                logger.warn("Rejected Firebase ID token: " + cause.getMessage());
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response, new BadCredentialsException("Invalid token", e));
                return;
            }
        }
        // No usable token: continue unauthenticated; the rules in SecurityConfig decide if that is allowed.
        chain.doFilter(request, response);
    }
}
