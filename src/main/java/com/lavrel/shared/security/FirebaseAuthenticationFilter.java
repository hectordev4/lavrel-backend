package com.lavrel.shared.security;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.domain.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
 * Controllers receive it with @AuthenticationPrincipal and only ever see IDs, never a token.
 *
 * Deliberately NOT a @Component: a Filter bean would also be registered in the servlet container
 * and run outside the security chain. SecurityConfig creates it and adds it to the chain.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

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

        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                AuthenticatedUser user = authenticateUser.authenticate(token);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(
                        user, null, AuthorityUtils.NO_AUTHORITIES));
                SecurityContextHolder.setContext(context);
            } catch (InvalidTokenException e) {
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response, new BadCredentialsException("Invalid token", e));
                return;
            }
        }
        // No header: continue unauthenticated; the rules in SecurityConfig decide if that is allowed.
        chain.doFilter(request, response);
    }
}
