package com.lavrel.shared.error;

import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.catalog.domain.GameNotOnPlatformException;
import com.lavrel.catalog.domain.InvalidComparisonException;
import com.lavrel.library.domain.ProfilePrivateException;
import com.lavrel.shared.domain.PlatformNotSupportedException;
import com.lavrel.shared.domain.PlatformUnavailableException;
import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.CountryNotFoundException;
import com.lavrel.user.domain.EmailAlreadyVerifiedException;
import com.lavrel.user.domain.EmailVerificationNotAllowedException;
import com.lavrel.user.domain.IdentityProviderUnavailableException;
import com.lavrel.user.domain.InvalidIdentityProofException;
import com.lavrel.user.domain.InvalidRoleChangeException;
import com.lavrel.user.domain.InvalidUsernameException;
import com.lavrel.user.domain.PlatformAccountNotFoundException;
import com.lavrel.user.domain.PublicUserNotFoundException;
import com.lavrel.user.domain.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountAlreadyLinkedException.class)
    public ResponseEntity<ApiError> accountAlreadyLinked(AccountAlreadyLinkedException e) {
        return error(HttpStatus.CONFLICT, "account_already_linked", e.getMessage());
    }

    @ExceptionHandler(PlatformAccountNotFoundException.class)
    public ResponseEntity<ApiError> platformAccountNotFound(PlatformAccountNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "platform_account_not_found", e.getMessage());
    }

    /** The internal id stays in the log; the client only learns that the account does not exist. */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> userNotFound(UserNotFoundException e) {
        log.info("User not found: internal id {}", e.userId());
        return error(HttpStatus.NOT_FOUND, "user_not_found", e.getMessage());
    }

    @ExceptionHandler(PublicUserNotFoundException.class)
    public ResponseEntity<ApiError> publicUserNotFound(PublicUserNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "user_not_found", e.getMessage());
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ApiError> gameNotFound(GameNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "game_not_found", e.getMessage());
    }

    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<ApiError> invalidUsername(InvalidUsernameException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_username", e.getMessage());
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

    /** The detail stays in the server log; the client only learns that the platform failed. */
    @ExceptionHandler(PlatformUnavailableException.class)
    public ResponseEntity<ApiError> platformUnavailable(PlatformUnavailableException e) {
        return error(HttpStatus.BAD_GATEWAY, "platform_unavailable",
                "The game platform could not be reached. Try again in a moment.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformedBody(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "malformed_request", "The request body is missing or not valid JSON");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParameter(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_parameter", "Invalid value for '" + e.getName() + "'");
    }

    @ExceptionHandler(PlatformNotSupportedException.class)
    public ResponseEntity<ApiError> platformNotSupported(PlatformNotSupportedException e) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "platform_not_supported", e.getMessage());
    }

    @ExceptionHandler(InvalidComparisonException.class)
    public ResponseEntity<ApiError> invalidComparison(InvalidComparisonException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_comparison", e.getMessage());
    }

    @ExceptionHandler(GameNotOnPlatformException.class)
    public ResponseEntity<ApiError> gameNotOnPlatform(GameNotOnPlatformException e) {
        return error(HttpStatus.NOT_FOUND, "game_not_on_platform", e.getMessage());
    }

    @ExceptionHandler(InvalidRoleChangeException.class)
    public ResponseEntity<ApiError> invalidRoleChange(InvalidRoleChangeException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_role_change", e.getMessage());
    }

    /** Deliberately the same body as an unknown path: the dev shortcut does not reveal that it exists. */
    @ExceptionHandler(EmailVerificationNotAllowedException.class)
    public ResponseEntity<ApiError> emailVerificationNotAllowed(EmailVerificationNotAllowedException e) {
        return error(HttpStatus.NOT_FOUND, "not_found", "Not found");
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ApiError> emailAlreadyVerified(EmailAlreadyVerifiedException e) {
        return error(HttpStatus.CONFLICT, "already_verified", e.getMessage());
    }

    @ExceptionHandler(IdentityProviderUnavailableException.class)
    public ResponseEntity<ApiError> identityProviderUnavailable(IdentityProviderUnavailableException e) {
        log.warn("Identity provider call failed: {}", e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        return error(HttpStatus.BAD_GATEWAY, "identity_provider_unavailable",
                "The sign-in service could not complete the change. Try again in a moment.");
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
