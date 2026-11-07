package com.lavrel.catalog.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** franchise_blocklist (migration V28): names an admin marked as not a franchise, by key (FranchiseRules.key). */
public interface FranchiseBlocklistPort {

    Set<String> keys();

    List<BlockedFranchise> list();

    void add(String key, String name, Instant at);

    boolean remove(String key);

    record BlockedFranchise(String key, String name, Instant addedAt) {
    }
}
