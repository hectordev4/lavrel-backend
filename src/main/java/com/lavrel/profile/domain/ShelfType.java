package com.lavrel.profile.domain;

/**
 * What a profile shelf shows. A shelf is the only way anything beyond the header becomes public: what is not on a
 * shelf is never shown.
 */
public enum ShelfType {
    /** One of the user's collections; putting it on a shelf is what makes it public. */
    COLLECTION(false),
    /** The last games played. */
    RECENTLY_PLAYED(true),
    /** Tracked games marked PLAYING. */
    PLAYING(true),
    /** Games with every achievement unlocked. */
    COMPLETED(true),
    /** Achievements the user picked by hand from their own unlocks. */
    ACHIEVEMENTS(false),
    /**
     * Games owned, achievements unlocked, games completed. The only place the number of games owned can appear, and
     * only with the profile setting showLibrarySize on.
     */
    STATS(true);

    private final boolean onlyOnce;

    ShelfType(boolean onlyOnce) {
        this.onlyOnce = onlyOnce;
    }

    /** A profile can have this kind of shelf only once. */
    public boolean onlyOnce() {
        return onlyOnce;
    }
}
