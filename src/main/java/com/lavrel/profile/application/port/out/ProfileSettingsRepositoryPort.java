package com.lavrel.profile.application.port.out;

import com.lavrel.profile.domain.ProfileSettings;
import java.util.Optional;

/** profile_settings (migration V22). No row means the defaults: everything private. */
public interface ProfileSettingsRepositoryPort {

    Optional<ProfileSettings> find(long userId);

    /** Inserts or updates. */
    void save(ProfileSettings settings);
}
