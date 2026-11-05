package com.lavrel.profile.application.port.in;

import com.lavrel.profile.application.port.in.ViewProfilesUseCase.Avatar;
import com.lavrel.profile.domain.AvatarStorageUnavailableException;
import com.lavrel.profile.domain.InvalidAvatarException;
import com.lavrel.profile.domain.ProfileNotFoundException;

/** The user's picture: generated (default), one from the gallery, or uploaded. */
public interface ManageAvatarUseCase {

    Avatar current(long userId);

    /**
     * Checks and stores the picture, then makes it the avatar; the previous upload, if any, is deleted.
     *
     * @throws InvalidAvatarException            if it is not an accepted picture
     * @throws AvatarStorageUnavailableException if storage is not configured or fails (nothing changes)
     */
    Avatar upload(long userId, byte[] picture);

    /** @throws InvalidAvatarException if the id is not in the gallery */
    Avatar chooseGallery(long userId, String galleryId);

    /** Back to the generated avatar; a stored upload is deleted. */
    Avatar reset(long userId);

    /**
     * Admin: removes someone's picture (moderation), back to the generated avatar.
     * @throws ProfileNotFoundException if no user has that public id
     */
    void removeByAdmin(long actorUserId, long targetPublicId);
}
