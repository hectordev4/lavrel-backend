package com.lavrel.profile.application.service;

import com.lavrel.profile.application.port.in.ManageAvatarUseCase;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.Avatar;
import com.lavrel.profile.application.port.out.AvatarImagePort;
import com.lavrel.profile.application.port.out.AvatarImagePort.PreparedAvatar;
import com.lavrel.profile.application.port.out.AvatarStoragePort;
import com.lavrel.profile.application.port.out.ProfileSettingsRepositoryPort;
import com.lavrel.profile.application.port.out.PublicProfileQueryPort;
import com.lavrel.profile.domain.AvatarKind;
import com.lavrel.profile.domain.ProfileNotFoundException;
import com.lavrel.profile.domain.ProfileSettings;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Upload order: check the picture, store it, save it as the avatar, and only then delete the previous one, so a
 * failure at any step leaves the user with a working avatar. Stored names are random (no user id in them).
 */
@Service
public class ManageAvatarService implements ManageAvatarUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageAvatarService.class);

    private final ProfileSettingsRepositoryPort settings;
    private final AvatarImagePort images;
    private final AvatarStoragePort storage;
    private final PublicProfileQueryPort users;

    public ManageAvatarService(ProfileSettingsRepositoryPort settings, AvatarImagePort images,
                               AvatarStoragePort storage, PublicProfileQueryPort users) {
        this.settings = settings;
        this.images = images;
        this.storage = storage;
        this.users = users;
    }

    @Override
    public Avatar current(long userId) {
        return view(load(userId));
    }

    @Override
    public Avatar upload(long userId, byte[] picture) {
        PreparedAvatar prepared = images.prepare(picture);
        String path = UUID.randomUUID() + "." + prepared.extension();
        storage.store(path, prepared.bytes(), prepared.contentType());
        ProfileSettings s = load(userId);
        String previous = s.uploadedPath();
        s.useUploadedAvatar(path);
        settings.save(s);
        deleteQuietly(previous);
        return view(s);
    }

    @Override
    public Avatar chooseGallery(long userId, String galleryId) {
        ProfileSettings s = load(userId);
        String previous = s.uploadedPath();
        s.chooseGalleryAvatar(galleryId);
        settings.save(s);
        deleteQuietly(previous);
        return view(s);
    }

    @Override
    public Avatar reset(long userId) {
        ProfileSettings s = load(userId);
        String previous = s.uploadedPath();
        s.resetAvatar();
        settings.save(s);
        deleteQuietly(previous);
        return view(s);
    }

    @Override
    public void removeByAdmin(long actorUserId, long targetPublicId) {
        long userId = users.findAnyUserId(targetPublicId).orElseThrow(ProfileNotFoundException::new);
        reset(userId);
        log.info("User {} removed the avatar of public id {}", actorUserId, targetPublicId);
    }

    private ProfileSettings load(long userId) {
        return settings.find(userId).orElseGet(() -> ProfileSettings.defaults(userId));
    }

    private void deleteQuietly(String path) {
        if (path != null) {
            storage.delete(path);
        }
    }

    private Avatar view(ProfileSettings s) {
        return switch (s.avatarKind()) {
            case GALLERY -> new Avatar(AvatarKind.GALLERY, s.avatarValue(), null);
            case UPLOADED -> new Avatar(AvatarKind.UPLOADED, null, storage.publicUrl(s.avatarValue()));
            case GENERATED -> Avatar.GENERATED;
        };
    }
}
