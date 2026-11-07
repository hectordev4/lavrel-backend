package com.lavrel.profile.application.port.in;

import com.lavrel.profile.domain.ProfileSettings;

/** The user's own profile settings. */
public interface ManageProfileUseCase {

    ProfileSettings settings(long userId);

    /** Partial update: null fields stay as they are. */
    ProfileSettings update(long userId, Boolean profilePublic, Boolean showCountry, Boolean showPlatforms,
                           Boolean showAvatar, Boolean showLibrarySize);
}
