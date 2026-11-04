package com.lavrel.user.adapter.in.web;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.GetPlatformAccountUseCase;
import com.lavrel.user.application.port.in.UnlinkPlatformAccountUseCase;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The platform accounts (Steam, Xbox, PSN) linked to the current user. */
@RestController
@RequestMapping("/api/currentuser/accounts")
public class CurrentUserAccountsController {

    private final GetPlatformAccountUseCase getPlatformAccounts;
    private final UnlinkPlatformAccountUseCase unlinkAccount;

    public CurrentUserAccountsController(GetPlatformAccountUseCase getPlatformAccounts,
                                         UnlinkPlatformAccountUseCase unlinkAccount) {
        this.getPlatformAccounts = getPlatformAccounts;
        this.unlinkAccount = unlinkAccount;
    }

    @GetMapping
    public List<AccountResponse> listAccounts(@AuthenticationPrincipal AuthenticatedUser user) {
        return getPlatformAccounts.getByUserId(user.userId()).stream()
                .map(a -> new AccountResponse(a.platform(), a.externalId(), a.displayName(), a.lastSyncedAt()))
                .toList();
    }

    /**
     * Unlinks an account: its games leave the library and the unlocks recorded from it are removed. 204; 404 if it
     * is not linked to you. Link and sync it again to bring everything back.
     */
    @DeleteMapping("/{platform}/{externalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable("platform") Platform platform,
                       @PathVariable("externalId") @Pattern(regexp = "[A-Za-z0-9_-]{1,128}",
                               message = "must be the platform's account id") String externalId) {
        unlinkAccount.unlink(user.userId(), platform, externalId);
    }

    /**
     * platform + externalId identify the account in the API (e.g. POST /accounts/steam/{externalId}/sync);
     * the database id is never exposed. lastSyncedAt is null until the first sync.
     */
    public record AccountResponse(Platform platform, String externalId, String displayName,
                                  Instant lastSyncedAt) {
    }
}
