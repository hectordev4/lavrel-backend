package user.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import shared.domain.Platform;

@Entity
@Table(name = "platform_account", uniqueConstraints = {@UniqueConstraint(columnNames = {"platform", "external_id"})})
public class PlatformAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected PlatformAccountJpaEntity() {
    }

    public PlatformAccountJpaEntity(Long id, long userId, Platform platform, String externalId, String displayName, boolean verified, Instant verifiedAt, Instant linkedAt, Instant lastSyncedAt) {
        this.id = id;
        this.userId = userId;
        this.platform = platform;
        this.externalId = externalId;
        this.displayName = displayName;
        this.verified = verified;
        this.verifiedAt = verifiedAt;
        this.linkedAt = linkedAt;
        this.lastSyncedAt = lastSyncedAt;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isVerified() {
        return verified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }
}
