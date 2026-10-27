package com.lavrel.user.adapter.in.web;

import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformNotSupportedException;
import com.lavrel.shared.domain.PlatformUnavailableException;
import com.lavrel.shared.error.ApiError;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.CreatePlatformLinkUrlUseCase;
import com.lavrel.user.application.port.in.LinkPlatformAccountUseCase;
import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.InvalidIdentityProofException;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.UserNotFoundException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Linking a platform account (Steam today; Xbox and PSN once their identity adapters exist), in two halves:
 *  1. POST /api/currentuser/accounts/{platform}/link (authenticated) returns the platform's sign-in URL.
 *  2. GET  /api/platforms/{platform}/callback (public; the platform redirects the browser here,
 *     identified by the signed state). /api/steam/callback keeps working for the URL already registered.
 * The controller never names a platform: the use cases pick the right adapter.
 *
 * With app.frontend.base-url set, the callback redirects the browser to
 * {base}/{platform}/linked?status=success (or ?status=error&reason=...). Without it (no frontend yet)
 * the callback answers with JSON, which is handy for testing from a browser.
 * The callback handles its own errors because a browser navigation needs a redirect, not a JSON error.
 */
@RestController
public class PlatformLinkController {

    private final CreatePlatformLinkUrlUseCase createLinkUrl;
    private final LinkPlatformAccountUseCase linkAccount;
    private final String frontendBaseUrl;

    public PlatformLinkController(CreatePlatformLinkUrlUseCase createLinkUrl,
                                  LinkPlatformAccountUseCase linkAccount,
                                  @Value("${app.frontend.base-url:}") String frontendBaseUrl) {
        this.createLinkUrl = createLinkUrl;
        this.linkAccount = linkAccount;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @PostMapping("/api/currentuser/accounts/{platform}/link")
    public LinkUrlResponse startLink(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable("platform") Platform platform) {
        return new LinkUrlResponse(createLinkUrl.createLoginUrl(user.userId(), platform));
    }

    @GetMapping("/api/platforms/{platform}/callback")
    public ResponseEntity<Object> callback(@PathVariable("platform") Platform platform,
                                           @RequestParam Map<String, String> params) {
        try {
            PlatformAccount account = linkAccount.link(
                    new LinkPlatformAccountUseCase.Command(platform, params.get("state"), params));
            return success(platform, account);
        } catch (AccountAlreadyLinkedException e) {
            return failure(platform, HttpStatus.CONFLICT, "account_already_linked", e.getMessage());
        } catch (InvalidIdentityProofException e) {
            return failure(platform, HttpStatus.BAD_REQUEST, "invalid_identity_proof", e.getMessage());
        } catch (UserNotFoundException e) {
            return failure(platform, HttpStatus.NOT_FOUND, "user_not_found", e.getMessage());
        } catch (PlatformNotSupportedException e) {
            return failure(platform, HttpStatus.UNPROCESSABLE_ENTITY, "platform_not_supported", e.getMessage());
        } catch (PlatformUnavailableException e) {
            return failure(platform, HttpStatus.BAD_GATEWAY, "platform_unavailable",
                    "The platform could not be reached. Try again in a moment.");
        }
    }

    /** The callback URL already registered with Steam. */
    @GetMapping("/api/steam/callback")
    public ResponseEntity<Object> steamCallback(@RequestParam Map<String, String> params) {
        return callback(Platform.STEAM, params);
    }

    private ResponseEntity<Object> success(Platform platform, PlatformAccount account) {
        if (frontendBaseUrl.isBlank()) {
            return ResponseEntity.ok(new LinkedResponse("linked", account.platform(), account.externalId()));
        }
        return redirect(UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(linkedPath(platform)).queryParam("status", "success").build().toUri());
    }

    private ResponseEntity<Object> failure(Platform platform, HttpStatus status, String code, String message) {
        if (frontendBaseUrl.isBlank()) {
            return ResponseEntity.status(status).body(new ApiError(code, message));
        }
        return redirect(UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(linkedPath(platform)).queryParam("status", "error").queryParam("reason", code)
                .build().toUri());
    }

    /** /steam/linked, /xbox/linked... (the Steam one is unchanged for the frontend). */
    private static String linkedPath(Platform platform) {
        return "/" + platform.name().toLowerCase(Locale.ROOT) + "/linked";
    }

    private static ResponseEntity<Object> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    public record LinkUrlResponse(String redirectUrl) {
    }

    public record LinkedResponse(String status, Platform platform, String externalId) {
    }
}
