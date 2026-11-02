package com.lavrel.library.adapter.in.web;

import com.lavrel.library.application.port.in.SyncAccountUseCase;
import com.lavrel.library.application.port.in.SyncAccountUseCase.SyncResult;
import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Starts a sync of one of the current user's platform accounts:
 * POST /api/currentuser/accounts/{platform}/{externalId}/sync, for example /accounts/steam/76561198000000000/sync.
 * The account is addressed by the platform's own id (public on the platform anyway), never by our database id.
 * The sync runs inside the request for now (fine for the MVP); a long library may later move to a background job.
 * Someone else's account answers 404, like one that does not exist.
 * When gamesPending is above 0, the user can sync again to import the rest of the library.
 */
@RestController
@RequestMapping("/api/currentuser/accounts")
public class AccountSyncController {

    private final SyncAccountUseCase syncAccount;

    public AccountSyncController(SyncAccountUseCase syncAccount) {
        this.syncAccount = syncAccount;
    }

    @PostMapping("/{platform}/{externalId}/sync")
    public SyncResponse sync(@AuthenticationPrincipal AuthenticatedUser user,
                             @PathVariable("platform") Platform platform,
                             @PathVariable("externalId") @Pattern(regexp = "[A-Za-z0-9_-]{1,128}",
                                     message = "must be the platform's account id") String externalId) {
        SyncResult result = syncAccount.sync(user.userId(), platform, externalId);
        return new SyncResponse(result.gamesSynced(), result.gamesSkipped(), result.unlocksRecorded(),
                result.gamesPending());
    }

    public record SyncResponse(int gamesSynced, int gamesSkipped, int unlocksRecorded, int gamesPending) {
    }
}
