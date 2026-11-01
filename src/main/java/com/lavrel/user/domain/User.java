package com.lavrel.user.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * MIXED. firebaseUid, email, emailVerified and photoUrl come from the sign-in account (Firebase) and are
 * refreshed when they change; the rest is owned by the app.
 * id is null until the user is persisted and is never shown outside the backend; publicId is the number
 * other people see. username is a free display name (not unique). countryId is optional: users without a
 * country are left out of country stats.
 * emailVerified is stored so the backend can query by it (admin lists, community features); permissions still
 * come from the token of each request, which is always at least as fresh.
 */
public record User(
        Long id,
        long publicId,
        String firebaseUid,
        String username,
        String email,
        boolean emailVerified,
        String photoUrl,
        Integer countryId,
        Role role,
        Instant createdAt) {

    public User {
        Objects.requireNonNull(firebaseUid, "firebaseUid");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(createdAt, "createdAt");
        if (!PublicIds.isValid(publicId)) {
            throw new IllegalArgumentException("publicId must have 16 digits");
        }
    }

    /** A user signing in for the first time: a USER with the starting name it was given. */
    public static User newcomer(long publicId, String firebaseUid, String username, String email,
                                boolean emailVerified, String photoUrl, Instant now) {
        return new User(null, publicId, firebaseUid, username, email, emailVerified, photoUrl, null, Role.USER, now);
    }

    /** True when the sign-in account says something different from what is stored. */
    public boolean signInDataDiffers(String newEmail, boolean newEmailVerified, String newPhotoUrl) {
        return !Objects.equals(email, newEmail) || emailVerified != newEmailVerified
                || !Objects.equals(photoUrl, newPhotoUrl);
    }

    public User withSignInData(String newEmail, boolean newEmailVerified, String newPhotoUrl) {
        return new User(id, publicId, firebaseUid, username, newEmail, newEmailVerified, newPhotoUrl, countryId,
                role, createdAt);
    }

    public User withUsername(String newUsername) {
        return new User(id, publicId, firebaseUid, newUsername, email, emailVerified, photoUrl, countryId, role,
                createdAt);
    }

    public User withCountry(Integer newCountryId) {
        return new User(id, publicId, firebaseUid, username, email, emailVerified, photoUrl, newCountryId, role,
                createdAt);
    }

    public User withRole(Role newRole) {
        return new User(id, publicId, firebaseUid, username, email, emailVerified, photoUrl, countryId, newRole,
                createdAt);
    }
}
