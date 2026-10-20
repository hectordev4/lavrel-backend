package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.MarkAccountSyncedUseCase;
import com.lavrel.user.application.port.out.PlatformAccountRepositoryPort;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.PlatformAccountNotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkAccountSyncedService implements MarkAccountSyncedUseCase {

    private final PlatformAccountRepositoryPort accounts;
    private final Clock clock;

    public MarkAccountSyncedService(PlatformAccountRepositoryPort accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void markSynced(long platformAccountId) {
        PlatformAccount account = accounts.findById(platformAccountId)
                .orElseThrow(() -> new PlatformAccountNotFoundException(platformAccountId));
        accounts.save(account.markSynced(clock.instant()));
    }
}
