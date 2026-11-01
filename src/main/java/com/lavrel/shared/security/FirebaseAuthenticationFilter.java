package com.lavrel.shared.security;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.domain.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer <Firebase ID token>", asks the user feature to authenticate it
 * (find or create the user) and stores the resulting AuthenticatedUser as the principal.
 * Authorities: an account with a verified e-mail gets EMAIL_VERIFIED plus ROLE_<its role> (USER, SUPPORT,
 * ADMIN, stored in the database). An unverified account gets none, so it can only reach the endpoints that
 * merely require authentication (its own profile); SecurityConfig has the rules.
 * Controllers receive the principal with @AuthenticationPrincipal and only ever see IDs, never a token.
 *
 * Deliberately NOT a @Component: a Filter bean would also be registered in the servlet container
 * and run outside the security chain. SecurityConfig creates it and adds it to the chain.
 *
 * The client always gets the same generic 401; the real reason is written to the server log
 * (never the token itself) so a rejected token can be diagnosed.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    /** Granted only to accounts whose e-mail is verified; required by almost every endpoint. */
    public static final String EMAIL_VERIFIED = "EMAIL_VERIFIED";

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticateUserUseCase authenticateUser;
    private final AuthenticationEntryPoint entryPoint;

    public FirebaseAuthenticationFilter(AuthenticateUserUseCase authenticateUser,
                                        AuthenticationEntryPoint entryPoint) {
        this.authenticateUser = authenticateUser;
        this.entryPoint = entryPoint;
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
                context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities(user)));
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

    private static List<GrantedAuthority> authorities(AuthenticatedUser user) {
        if (!user.emailVerified()) {
            return AuthorityUtils.NO_AUTHORITIES;
        }
        return AuthorityUtils.createAuthorityList(EMAIL_VERIFIED, "ROLE_" + user.role().name());
    }
}
