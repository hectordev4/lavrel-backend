package com.lavrel.profile.application.port.out;

import com.lavrel.profile.application.port.in.ViewProfilesUseCase.ProfileHeader;
import java.util.List;
import java.util.Optional;

/**
 * Read-only queries over public profiles only. They read users, country and platform_account (other features' tables,
 * the documented read-model exception) and apply each user's opt-ins in the query, so nothing private ever leaves the
 * database.
 */
public interface PublicProfileQueryPort {

    /** Empty when the user does not exist or the profile is not public. */
    Optional<ProfileHeader> findHeader(long publicId);

    /** The internal user id behind a public profile, to build its shelves. Empty if not public. */
    Optional<Long> findUserId(long publicId);

    /** Any user's internal id by public id, public profile or not. For admin tools only. */
    Optional<Long> findAnyUserId(long publicId);

    /** By name (accent and case insensitive) or, when exactPublicId is set, that one profile. */
    List<ProfileHeader> search(String query, Long exactPublicId, int limit, long offset);
}
