package com.lavrel.library.application.port.out;

import java.time.Instant;
import java.util.List;

/**
 * Accounts the automatic sync should look at: their user has automatic sync on and used the app since seenAfter,
 * and the account was last synced before syncedBefore (or never). Longest waiting first.
 * Reads platform_account and users: the documented read-model exception.
 */
public interface DueAccountsQueryPort {

    List<Long> findDue(Instant syncedBefore, Instant seenAfter, int limit);
}
