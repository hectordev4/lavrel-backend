package com.lavrel.user.domain;

import java.util.random.RandomGenerator;

/**
 * The number that identifies a user in public (profile addresses, community search), like Steam's account
 * number. Random on purpose: the database id is sequential and would reveal how many users there are and let
 * anyone walk through every profile. Always 16 digits, so it never starts with 0 and fits in a long.
 * The same range is the column default and CHECK constraint in migration V12; tooling such as the fake data
 * relies on that default instead of repeating the range.
 */
public final class PublicIds {

    public static final long MIN = 1_000_000_000_000_000L;
    public static final long MAX = 9_999_999_999_999_999L;

    private PublicIds() {
    }

    /** One candidate; uniqueness is checked by PublicIdAllocator (and enforced by the database). */
    public static long draw(RandomGenerator random) {
        return random.nextLong(MIN, MAX + 1);
    }

    public static boolean isValid(long publicId) {
        return publicId >= MIN && publicId <= MAX;
    }
}
