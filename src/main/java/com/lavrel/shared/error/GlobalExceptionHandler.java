package com.lavrel.shared.error;

import com.lavrel.library.domain.ProfilePrivateException;
import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.CountryNotFoundException;
import com.lavrel.user.domain.InvalidIdentityProofException;
import com.lavrel.user.domain.PlatformAccountNotFoundException;
import com.lavrel.user.domain.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates domain exceptions into HTTP responses. There is deliberately no catch-all
 * Exception handler: it would turn Spring's own 404/405 responses into 500s.
 * Unexpected errors fall through to Spring Boot's default 500 response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountAlreadyLinkedException.class)
    public ResponseEntity<ApiError> accountAlreadyLinked(AccountAlreadyLinkedException e) {
        return error(HttpStatus.CONFLICT, "account_already_linked", e.getMessage());
    }

    @ExceptionHandler(PlatformAccountNotFoundException.class)
    public ResponseEntity<ApiError> platformAccountNotFound(PlatformAccountNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "platform_account_not_found", e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> userNotFound(UserNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "user_not_found", e.getMessage());
    }

    @ExceptionHandler(CountryNotFoundException.class)
    public ResponseEntity<ApiError> countryNotFound(CountryNotFoundException e) {
        return error(HttpStatus.BAD_REQUEST, "country_not_found", e.getMessage());
    }

    @ExceptionHandler(ProfilePrivateException.class)
    public ResponseEntity<ApiError> profilePrivate(ProfilePrivateException e) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "profile_private",
                "The platform profile is private. Make the game details public and try again.");
    }

    @ExceptionHandler(InvalidIdentityProofException.class)
    public ResponseEntity<ApiError> invalidIdentityProof(InvalidIdentityProofException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_identity_proof", e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformedBody(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "malformed_request", "The request body is missing or not valid JSON");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParameter(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_parameter", "Invalid value for '" + e.getName() + "'");
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
