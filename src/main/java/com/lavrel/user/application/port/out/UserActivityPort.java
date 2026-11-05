package com.lavrel.user.application.port.out;

import java.time.Instant;

/** users.last_seen_at and users.auto_sync (migration V21). */
public interface UserActivityPort {

    /** Records that the user used the app. Cheap to call on every request: it writes at most once an hour. */
    void recordSeen(long userId, Instant at);

    boolean autoSyncEnabled(long userId);

    void setAutoSync(long userId, boolean enabled);
}
