package com.lavrel.user.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.user.application.port.in.GetPlatformAccountUseCase;
import com.lavrel.user.application.port.out.PlatformAccountRepositoryPort;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.PlatformAccountNotFoundException;

@Service
@Transactional(readOnly = true)
public class GetPlatformAccountService implements GetPlatformAccountUseCase {

    private final PlatformAccountRepositoryPort accounts;

    public GetPlatformAccountService(PlatformAccountRepositoryPort accounts) {
        this.accounts = accounts;
    }

    @Override
    public PlatformAccountView getById(long platformAccountId) {
        return accounts.findById(platformAccountId)
                .map(GetPlatformAccountService::toView)
                .orElseThrow(() -> new PlatformAccountNotFoundException(platformAccountId));
    }

    @Override
    public List<PlatformAccountView> getByUserId(long userId) {
        return accounts.findByUserId(userId).stream().map(GetPlatformAccountService::toView).toList();
    }

    private static PlatformAccountView toView(PlatformAccount a) {
        return new PlatformAccountView(a.id(), a.userId(), a.platform(), a.externalId());
    }
}
