package com.lavrel.profile.application.port.in;

import com.lavrel.profile.domain.ProfileNotFoundException;
import com.lavrel.shared.domain.Platform;
import java.time.YearMonth;
import java.util.List;

/** Public profiles as other people see them, and the player search. Only profiles their owner made public. */
public interface ViewProfilesUseCase {

    /** @throws ProfileNotFoundException if there is no public profile with that id */
    ProfileHeader header(long publicId);

    /**
     * query: 2+ characters of a name, or an exact 16-digit public id.
     */
    List<ProfileHeader> search(String query, int page);

    /** country and platforms are null / empty unless the user opted into showing them. */
    record ProfileHeader(long publicId, String username, YearMonth memberSince, Country country,
                         List<Platform> platforms) {
    }

    record Country(String code, String name) {
    }
}
