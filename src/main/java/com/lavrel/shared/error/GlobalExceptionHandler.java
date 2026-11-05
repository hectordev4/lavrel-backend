package com.lavrel.shared.error;

import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.catalog.domain.GameNotOnPlatformException;
import com.lavrel.catalog.domain.InvalidComparisonException;
import com.lavrel.library.domain.CollectionLimitReachedException;
import com.lavrel.library.domain.CollectionNameTakenException;
import com.lavrel.library.domain.CollectionNotFoundException;
import com.lavrel.library.domain.GameNotOwnedException;
import com.lavrel.library.domain.InvalidCollectionNameException;
import com.lavrel.library.domain.InvalidCollectionOrderException;
import com.lavrel.library.domain.ProfilePrivateException;
import com.lavrel.profile.domain.ProfileNotFoundException;
import com.lavrel.shared.domain.PlatformNotSupportedException;
import com.lavrel.shared.domain.PlatformUnavailableException;
import com.lavrel.shared.error.ApiError.FieldProblem;
import com.lavrel.tracking.domain.GameNotTrackedException;
import com.lavrel.tracking.domain.InvalidQuickAccessOrderException;
import com.lavrel.tracking.domain.QuickAccessFullException;
import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.CountryNotFoundException;
import com.lavrel.user.domain.EmailAlreadyVerifiedException;
import com.lavrel.user.domain.EmailVerificationNotAllowedException;
import com.lavrel.user.domain.IdentityProviderUnavailableException;
import com.lavrel.user.domain.InvalidIdentityProofException;
import com.lavrel.user.domain.InvalidRoleChangeException;
import com.lavrel.user.domain.InvalidUsernameException;
import com.lavrel.user.domain.LastAdminException;
import com.lavrel.user.domain.PlatformAccountNotFoundException;
import com.lavrel.user.domain.PublicUserNotFoundException;
import com.lavrel.user.domain.UserNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions into HTTP responses with an ApiError body.
 *  400 the request itself is wrong (malformed JSON, invalid fields or parameters, invalid domain values)
 *  401/403 come from the security layer (JsonAuthenticationEntryPoint, JsonAccessDeniedHandler)
 *  404 something the client referred to does not exist (or is not theirs)
 *  409 the request conflicts with existing data (account already linked)
 *  422 well-formed, but cannot be processed now (private profile, platform not supported yet)
 *  502 a platform API or the identity provider failed
 * There is deliberately no catch-all Exception handler: it would turn Spring's own 404/405 responses into 500s.
 * Unexpected errors fall through to Spring Boot's default 500 response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------------------------------------------------------- 400: invalid request

    /** @Valid on a @RequestBody failed: one entry per invalid field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e) {
        List<FieldProblem> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldProblem(f.getField(), message(f)))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "validation_failed", "Some fields are invalid", details);
    }

    /** Constraints on @PathVariable / @RequestParam (or a container of them) failed. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> invalidParameters(HandlerMethodValidationException e) {
        List<FieldProblem> details = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(err -> new FieldProblem(result.getMethodParameter().getParameterName(), message(err))))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "validation_failed", "Some parameters are invalid", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformedBody(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "malformed_request", "The request body is missing or not valid JSON");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParameter(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_parameter", "Invalid value for '" + e.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> missingParameter(MissingServletRequestParameterException e) {
        return error(HttpStatus.BAD_REQUEST, "missing_parameter", "Missing parameter '" + e.getParameterName() + "'");
    }

    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<ApiError> invalidUsername(InvalidUsernameException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_username", e.getMessage());
    }

    @ExceptionHandler(CountryNotFoundException.class)
    public ResponseEntity<ApiError> countryNotFound(CountryNotFoundException e) {
        return error(HttpStatus.BAD_REQUEST, "country_not_found", e.getMessage());
    }

    @ExceptionHandler(InvalidIdentityProofException.class)
    public ResponseEntity<ApiError> invalidIdentityProof(InvalidIdentityProofException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_identity_proof", e.getMessage());
    }

    @ExceptionHandler(InvalidRoleChangeException.class)
    public ResponseEntity<ApiError> invalidRoleChange(InvalidRoleChangeException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_role_change", e.getMessage());
    }

    @ExceptionHandler(InvalidQuickAccessOrderException.class)
    public ResponseEntity<ApiError> invalidQuickAccessOrder(InvalidQuickAccessOrderException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_quick_access_order", e.getMessage());
    }

    @ExceptionHandler(InvalidCollectionNameException.class)
    public ResponseEntity<ApiError> invalidCollectionName(InvalidCollectionNameException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_collection_name", e.getMessage());
    }

    @ExceptionHandler(InvalidCollectionOrderException.class)
    public ResponseEntity<ApiError> invalidCollectionOrder(InvalidCollectionOrderException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_collection_order", e.getMessage());
    }

    @ExceptionHandler(InvalidComparisonException.class)
    public ResponseEntity<ApiError> invalidComparison(InvalidComparisonException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_comparison", e.getMessage());
    }

    // ---------------------------------------------------------------- 404: not found

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

    @ExceptionHandler(GameNotTrackedException.class)
    public ResponseEntity<ApiError> gameNotTracked(GameNotTrackedException e) {
        return error(HttpStatus.NOT_FOUND, "game_not_tracked", e.getMessage());
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiError> profileNotFound(ProfileNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "profile_not_found", e.getMessage());
    }

    @ExceptionHandler(CollectionNotFoundException.class)
    public ResponseEntity<ApiError> collectionNotFound(CollectionNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "collection_not_found", e.getMessage());
    }

    @ExceptionHandler(GameNotOnPlatformException.class)
    public ResponseEntity<ApiError> gameNotOnPlatform(GameNotOnPlatformException e) {
        return error(HttpStatus.NOT_FOUND, "game_not_on_platform", e.getMessage());
    }

    /** Deliberately the same body as an unknown path: the dev shortcut does not reveal that it exists. */
    @ExceptionHandler(EmailVerificationNotAllowedException.class)
    public ResponseEntity<ApiError> emailVerificationNotAllowed(EmailVerificationNotAllowedException e) {
        return error(HttpStatus.NOT_FOUND, "not_found", "Not found");
    }

    // ---------------------------------------------------------------- 409: conflict

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ApiError> emailAlreadyVerified(EmailAlreadyVerifiedException e) {
        return error(HttpStatus.CONFLICT, "already_verified", e.getMessage());
    }

    @ExceptionHandler(QuickAccessFullException.class)
    public ResponseEntity<ApiError> quickAccessFull(QuickAccessFullException e) {
        return error(HttpStatus.CONFLICT, "quick_access_full", e.getMessage());
    }

    @ExceptionHandler(CollectionNameTakenException.class)
    public ResponseEntity<ApiError> collectionNameTaken(CollectionNameTakenException e) {
        return error(HttpStatus.CONFLICT, "collection_name_taken", e.getMessage());
    }

    @ExceptionHandler(CollectionLimitReachedException.class)
    public ResponseEntity<ApiError> collectionLimitReached(CollectionLimitReachedException e) {
        return error(HttpStatus.CONFLICT, "collection_limit_reached", e.getMessage());
    }

    @ExceptionHandler(LastAdminException.class)
    public ResponseEntity<ApiError> lastAdmin(LastAdminException e) {
        return error(HttpStatus.CONFLICT, "last_admin", e.getMessage());
    }

    @ExceptionHandler(AccountAlreadyLinkedException.class)
    public ResponseEntity<ApiError> accountAlreadyLinked(AccountAlreadyLinkedException e) {
        return error(HttpStatus.CONFLICT, "account_already_linked", e.getMessage());
    }

    // ---------------------------------------------------------------- 422: cannot be processed now

    @ExceptionHandler(ProfilePrivateException.class)
    public ResponseEntity<ApiError> profilePrivate(ProfilePrivateException e) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "profile_private",
                "The platform profile is private. Make the game details public and try again.");
    }

    @ExceptionHandler(GameNotOwnedException.class)
    public ResponseEntity<ApiError> gameNotOwned(GameNotOwnedException e) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "game_not_owned", e.getMessage());
    }

    @ExceptionHandler(PlatformNotSupportedException.class)
    public ResponseEntity<ApiError> platformNotSupported(PlatformNotSupportedException e) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "platform_not_supported", e.getMessage());
    }

    // ---------------------------------------------------------------- 502: platform failure

    /** The detail stays in the server log; the client only learns that the platform failed. */
    @ExceptionHandler(PlatformUnavailableException.class)
    public ResponseEntity<ApiError> platformUnavailable(PlatformUnavailableException e) {
        log.warn("Platform call failed: {}", e.getMessage());
        return error(HttpStatus.BAD_GATEWAY, "platform_unavailable",
                "The game platform could not be reached. Try again in a moment.");
    }

    @ExceptionHandler(IdentityProviderUnavailableException.class)
    public ResponseEntity<ApiError> identityProviderUnavailable(IdentityProviderUnavailableException e) {
        log.warn("Identity provider call failed: {}", e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        return error(HttpStatus.BAD_GATEWAY, "identity_provider_unavailable",
                "The sign-in service could not complete the change. Try again in a moment.");
    }

    private static String message(MessageSourceResolvable error) {
        return error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage();
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return error(status, code, message, List.of());
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                                  List<FieldProblem> details) {
        return ResponseEntity.status(status).body(new ApiError(code, message, details));
    }
}
