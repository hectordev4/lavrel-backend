package com.lavrel.user.adapter.in.web;

import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformUnavailableException;
import com.lavrel.shared.error.ApiError;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.CreateSteamLinkUrlUseCase;
import com.lavrel.user.application.port.in.LinkSteamAccountUseCase;
import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.InvalidIdentityProofException;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.UserNotFoundException;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The two halves of linking a Steam account:
 *  1. POST /api/currentuser/accounts/steam/link (authenticated) returns the Steam login URL.
 *  2. GET  /api/steam/callback (public; Steam redirects the browser here, identified by the signed state).
 *
 * With app.frontend.base-url set, the callback redirects the browser to
 * {base}/steam/linked?status=success (or ?status=error&reason=...). Without it (no frontend yet)
 * the callback answers with JSON, which is handy for testing from a browser.
 */
@RestController
public class SteamLinkController {

    private final CreateSteamLinkUrlUseCase createLinkUrl;
    private final LinkSteamAccountUseCase linkSteamAccount;
    private final String frontendBaseUrl;

    public SteamLinkController(CreateSteamLinkUrlUseCase createLinkUrl,
                               LinkSteamAccountUseCase linkSteamAccount,
                               @Value("${app.frontend.base-url:}") String frontendBaseUrl) {
        this.createLinkUrl = createLinkUrl;
        this.linkSteamAccount = linkSteamAccount;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @PostMapping("/api/currentuser/accounts/steam/link")
    public LinkUrlResponse startLink(@AuthenticationPrincipal AuthenticatedUser user) {
        return new LinkUrlResponse(createLinkUrl.createLoginUrl(user.userId()));
    }

    @GetMapping("/api/steam/callback")
    public ResponseEntity<Object> callback(@RequestParam Map<String, String> params) {
        try {
            PlatformAccount account = linkSteamAccount.link(
                    new LinkSteamAccountUseCase.Command(params.get("state"), params));
            return success(account);
        } catch (AccountAlreadyLinkedException e) {
            return failure(HttpStatus.CONFLICT, "account_already_linked", e.getMessage());
        } catch (InvalidIdentityProofException e) {
            return failure(HttpStatus.BAD_REQUEST, "invalid_identity_proof", e.getMessage());
        } catch (UserNotFoundException e) {
            return failure(HttpStatus.NOT_FOUND, "user_not_found", e.getMessage());
        } catch (PlatformUnavailableException e) {
            return failure(HttpStatus.BAD_GATEWAY, "platform_unavailable",
                    "Steam could not be reached. Try again in a moment.");
        }
    }

    private ResponseEntity<Object> success(PlatformAccount account) {
        if (frontendBaseUrl.isBlank()) {
            return ResponseEntity.ok(new LinkedResponse("linked", account.platform(), account.externalId()));
        }
        return redirect(UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/steam/linked").queryParam("status", "success").build().toUri());
    }

    private ResponseEntity<Object> failure(HttpStatus status, String code, String message) {
        if (frontendBaseUrl.isBlank()) {
            return ResponseEntity.status(status).body(new ApiError(code, message));
        }
        return redirect(UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/steam/linked").queryParam("status", "error").queryParam("reason", code).build().toUri());
    }

    private static ResponseEntity<Object> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    public record LinkUrlResponse(String redirectUrl) {
    }

    public record LinkedResponse(String status, Platform platform, String externalId) {
    }
}
