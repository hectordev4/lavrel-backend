package com.yourapp.user.adapter.out.persistence.mapper;

import com.yourapp.user.adapter.out.persistence.entity.PlatformAccountJpaEntity;
import com.yourapp.user.domain.PlatformAccount;

public final class PlatformAccountMapper {

    private PlatformAccountMapper() {
    }

    public static PlatformAccount toDomain(PlatformAccountJpaEntity e) {
        return new PlatformAccount(e.getId(), e.getUserId(), e.getPlatform(), e.getExternalId(), e.getDisplayName(),
                e.isVerified(), e.getVerifiedAt(), e.getLinkedAt(), e.getLastSyncedAt());
    }

    public static PlatformAccountJpaEntity toEntity(PlatformAccount d) {
        return new PlatformAccountJpaEntity(d.id(), d.userId(), d.platform(), d.externalId(), d.displayName(),
                d.verified(), d.verifiedAt(), d.linkedAt(), d.lastSyncedAt());
    }
}
