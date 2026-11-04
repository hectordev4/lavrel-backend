package com.lavrel.user.application.service;

import com.lavrel.progress.application.port.in.RecordUnlocksUseCase;
import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.in.UnlinkPlatformAccountUseCase;
import com.lavrel.user.application.port.out.PlatformAccountRepositoryPort;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.PlatformAccountNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The unlocks go first, while the account still exists to tell which ones came from it. */
@Service
public class UnlinkPlatformAccountService implements UnlinkPlatformAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(UnlinkPlatformAccountService.class);

    private final PlatformAccountRepositoryPort accounts;
    private final RecordUnlocksUseCase unlocks;

    public UnlinkPlatformAccountService(PlatformAccountRepositoryPort accounts, RecordUnlocksUseCase unlocks) {
        this.accounts = accounts;
        this.unlocks = unlocks;
    }

    @Override
    @Transactional
    public void unlink(long userId, Platform platform, String externalId) {
        PlatformAccount account = accounts.findByPlatformAndExternalId(platform, externalId)
                .filter(a -> a.belongsTo(userId))
                .orElseThrow(() -> new PlatformAccountNotFoundException(platform, externalId));
        int removed = unlocks.forgetAccount(userId, account.id());
        accounts.delete(account.id());
        log.info("User {} unlinked a {} account ({} unlocks removed)", userId, platform, removed);
    }
}
