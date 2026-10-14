package com.lavrel.user.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * MIXED. firebaseUid, email and photoUrl come from Firebase; the rest is owned by the app.
 * id is null until the user is persisted. countryId is optional: users without a country
 * are left out of country stats.
 */
public record User(
        Long id,
        String firebaseUid,
        String username,
        String email,
        String photoUrl,
        Integer countryId,
        Instant createdAt) {

    public User {
        Objects.requireNonNull(firebaseUid, "firebaseUid");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public User withCountry(Integer newCountryId) {
        return new User(id, firebaseUid, username, email, photoUrl, newCountryId, createdAt);
    }

    public User withProfile(String newUsername, String newEmail, String newPhotoUrl) {
        return new User(id, firebaseUid, newUsername, newEmail, newPhotoUrl, countryId, createdAt);
    }
}
