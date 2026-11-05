package com.lavrel.profile.application.service;

import com.lavrel.profile.application.port.in.ManageProfileUseCase;
import com.lavrel.profile.application.port.out.ProfileSettingsRepositoryPort;
import com.lavrel.profile.domain.ProfileSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageProfileService implements ManageProfileUseCase {

    private final ProfileSettingsRepositoryPort settings;

    public ManageProfileService(ProfileSettingsRepositoryPort settings) {
        this.settings = settings;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileSettings settings(long userId) {
        return settings.find(userId).orElseGet(() -> ProfileSettings.defaults(userId));
    }

    @Override
    @Transactional
    public ProfileSettings update(long userId, Boolean profilePublic, Boolean showCountry, Boolean showPlatforms,
                                  Boolean showAvatar) {
        ProfileSettings current = settings(userId);
        if (current.update(profilePublic, showCountry, showPlatforms, showAvatar)) {
            settings.save(current);
        }
        return current;
    }
}
