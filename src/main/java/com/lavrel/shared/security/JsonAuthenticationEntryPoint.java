package com.lavrel.shared.security;

import com.lavrel.shared.error.ApiError;
import com.lavrel.shared.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Single place that builds the 401 response, used both by the filter and by Spring Security. */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ApiError UNAUTHORIZED =
            new ApiError("unauthorized", "Missing or invalid Firebase ID token");

    private final ErrorResponseWriter writer;

    public JsonAuthenticationEntryPoint(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        writer.write(response, HttpStatus.UNAUTHORIZED, UNAUTHORIZED);
    }
}
