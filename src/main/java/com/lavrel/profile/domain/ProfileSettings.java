package com.lavrel.profile.domain;

/**
 * What a user lets other people see. Everything is off until the user turns it on: a profile that is not public does
 * not exist for anybody else (404, never in search). Even a public profile shows only the username, the public id and
 * the month the user joined, plus each item the user opted into here and the shelves they built.
 *
 * An entity: it changes over time through {@link #update}. {@link #defaults} is the state of someone who never touched
 * their profile.
 */
public class ProfileSettings {

    private final long userId;
    private boolean profilePublic;
    private boolean showCountry;
    private boolean showPlatforms;
    private boolean showAvatar;
    private AvatarKind avatarKind;
    private String avatarValue;

    private ProfileSettings(long userId, boolean profilePublic, boolean showCountry, boolean showPlatforms,
                            boolean showAvatar, AvatarKind avatarKind, String avatarValue) {
        this.userId = userId;
        this.profilePublic = profilePublic;
        this.showCountry = showCountry;
        this.showPlatforms = showPlatforms;
        this.showAvatar = showAvatar;
        this.avatarKind = avatarKind == null ? AvatarKind.GENERATED : avatarKind;
        this.avatarValue = avatarValue;
    }

    /** Everything private. */
    public static ProfileSettings defaults(long userId) {
        return new ProfileSettings(userId, false, false, false, false, AvatarKind.GENERATED, null);
    }

    public static ProfileSettings restore(long userId, boolean profilePublic, boolean showCountry,
                                          boolean showPlatforms, boolean showAvatar, AvatarKind avatarKind,
                                          String avatarValue) {
        return new ProfileSettings(userId, profilePublic, showCountry, showPlatforms, showAvatar, avatarKind,
                avatarValue);
    }

    /** @throws InvalidAvatarException if the id is not in the gallery */
    public void chooseGalleryAvatar(String galleryId) {
        if (!AvatarGallery.contains(galleryId)) {
            throw new InvalidAvatarException("Unknown gallery picture: " + galleryId);
        }
        avatarKind = AvatarKind.GALLERY;
        avatarValue = galleryId;
    }

    /** storagePath: where the stored picture is (Supabase Storage). */
    public void useUploadedAvatar(String storagePath) {
        avatarKind = AvatarKind.UPLOADED;
        avatarValue = storagePath;
    }

    /** Back to the generated avatar. */
    public void resetAvatar() {
        avatarKind = AvatarKind.GENERATED;
        avatarValue = null;
    }

    /** The stored picture's path when the avatar is an upload, otherwise null (nothing to delete). */
    public String uploadedPath() {
        return avatarKind == AvatarKind.UPLOADED ? avatarValue : null;
    }

    /** Partial update: null keeps the current value. @return true if anything changed */
    public boolean update(Boolean newPublic, Boolean newShowCountry, Boolean newShowPlatforms, Boolean newShowAvatar) {
        boolean before = profilePublic;
        boolean country = showCountry;
        boolean platforms = showPlatforms;
        boolean avatar = showAvatar;
        if (newPublic != null) {
            profilePublic = newPublic;
        }
        if (newShowCountry != null) {
            showCountry = newShowCountry;
        }
        if (newShowPlatforms != null) {
            showPlatforms = newShowPlatforms;
        }
        if (newShowAvatar != null) {
            showAvatar = newShowAvatar;
        }
        return before != profilePublic || country != showCountry || platforms != showPlatforms
                || avatar != showAvatar;
    }

    public long userId() {
        return userId;
    }

    public boolean profilePublic() {
        return profilePublic;
    }

    public boolean showCountry() {
        return showCountry;
    }

    /** Badges like "plays on Steam"; never account names or ids. */
    public boolean showPlatforms() {
        return showPlatforms;
    }

    /** The chosen or uploaded picture; otherwise others see the generated avatar. */
    public boolean showAvatar() {
        return showAvatar;
    }

    public AvatarKind avatarKind() {
        return avatarKind;
    }

    /** The gallery id (GALLERY) or the storage path (UPLOADED); null for GENERATED. */
    public String avatarValue() {
        return avatarValue;
    }
}
