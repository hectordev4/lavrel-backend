package com.lavrel.shared.security;

import com.lavrel.shared.error.ApiError;
import com.lavrel.shared.error.ErrorResponseWriter;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
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

    private static final ApiError EMAIL_NOT_VERIFIED = new ApiError("email_not_verified",
            "Verify your e-mail address to continue. Check your inbox for the verification link.");
    private static final ApiError FORBIDDEN = new ApiError("forbidden", "You do not have permission to do this");

    private final ErrorResponseWriter writer;

    public JsonAccessDeniedHandler(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        writer.write(response, HttpStatus.FORBIDDEN, isUnverified() ? EMAIL_NOT_VERIFIED : FORBIDDEN);
    }

    private static boolean isUnverified() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                && !user.emailVerified();
    }
}
