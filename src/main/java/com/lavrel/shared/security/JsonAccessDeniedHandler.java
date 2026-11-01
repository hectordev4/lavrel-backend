package com.lavrel.shared.security;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Single place that builds the 403 response. An account whose e-mail is not verified gets
 * email_not_verified, so the frontend can show "check your inbox"; anything else is a plain forbidden.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean unverified = authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                && !user.emailVerified();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(unverified
                ? "{\"error\":\"email_not_verified\",\"message\":\"Verify your e-mail address to continue. "
                  + "Check your inbox for the verification link.\"}"
                : "{\"error\":\"forbidden\",\"message\":\"You do not have permission to do this\"}");
    }
}
