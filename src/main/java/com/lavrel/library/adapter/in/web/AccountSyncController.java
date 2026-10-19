package com.lavrel.library.adapter.in.web;

import com.lavrel.library.application.port.in.SyncAccountUseCase;
import com.lavrel.library.application.port.in.SyncAccountUseCase.SyncResult;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Starts a sync of one of the current user's platform accounts. The sync runs inside the request
 * for now (fine for the MVP); a long library may later be moved to a background job.
 * The ownership check lives in the use case: someone else's account returns 404.
 */
@RestController
@RequestMapping("/api/currentuser/accounts")
public class AccountSyncController {

    private final SyncAccountUseCase syncAccount;

    public AccountSyncController(SyncAccountUseCase syncAccount) {
        this.syncAccount = syncAccount;
    }

    @PostMapping("/{accountId}/sync")
    public SyncResponse sync(@AuthenticationPrincipal AuthenticatedUser user,
                             @PathVariable("accountId") long accountId) {
        SyncResult result = syncAccount.sync(user.userId(), accountId);
        return new SyncResponse(result.gamesSynced(), result.gamesSkipped(), result.unlocksRecorded());
    }

    public record SyncResponse(int gamesSynced, int gamesSkipped, int unlocksRecorded) {
    }
}
