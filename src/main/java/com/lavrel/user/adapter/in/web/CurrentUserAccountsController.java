package com.lavrel.user.adapter.in.web;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.GetPlatformAccountUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The platform accounts (Steam, Xbox, PSN) linked to the current user. */
@RestController
@RequestMapping("/api/currentuser/accounts")
public class CurrentUserAccountsController {

    private final GetPlatformAccountUseCase getPlatformAccounts;

    public CurrentUserAccountsController(GetPlatformAccountUseCase getPlatformAccounts) {
        this.getPlatformAccounts = getPlatformAccounts;
    }

    @GetMapping
    public List<AccountResponse> listAccounts(@AuthenticationPrincipal AuthenticatedUser user) {
        return getPlatformAccounts.getByUserId(user.userId()).stream()
                .map(a -> new AccountResponse(a.id(), a.platform(), a.externalId(), a.displayName(), a.lastSyncedAt()))
                .toList();
    }

    /** lastSyncedAt is null until the first sync. */
    public record AccountResponse(long id, Platform platform, String externalId, String displayName,
                                  Instant lastSyncedAt) {
    }
}
