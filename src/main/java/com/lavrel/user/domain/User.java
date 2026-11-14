package com.lavrel.user.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A person using Lavrel. firebaseUid, email, emailVerified and photoUrl come from the sign-in account (Firebase)
 * and are refreshed when they change there; the rest is ours. email and photoUrl are PRIVATE: only the user
 * themself ever sees them.
 *  - id: the database id, null until stored, never shown outside the backend;
 *  - publicId: the 16-digit number other people see;
 *  - username: a free display name, not unique (see Usernames);
 *  - countryId: optional; users without one are left out of country statistics;
 *  - emailVerified: stored for queries; permissions come from the token of each request;
 *  - setupCompletedAt: when the user finished (or skipped) the first-setup screen; null until then.
 *
 * An entity: its identity is the Firebase account, and it changes over time through its methods, which keep it
 * valid (an invalid change leaves it as it was). Create it with {@link #register}; the persistence adapter rebuilds
 * stored ones with {@link #restore}.
 */
public class User {

    private final Long id;
    private final long publicId;
    private final String firebaseUid;
    private String username;
    private String email;
    private boolean emailVerified;
    private String photoUrl;
    private Integer countryId;
    private Role role;
    private final Instant createdAt;
    private Instant setupCompletedAt;

    private User(Long id, long publicId, String firebaseUid, String username, String email, boolean emailVerified,
                 String photoUrl, Integer countryId, Role role, Instant createdAt, Instant setupCompletedAt) {
        if (!PublicIds.isValid(publicId)) {
            throw new IllegalArgumentException("publicId must have 16 digits");
        }
        this.id = id;
        this.publicId = publicId;
        this.firebaseUid = Objects.requireNonNull(firebaseUid, "firebaseUid");
        this.username = Objects.requireNonNull(username, "username");
        this.email = email;
        this.emailVerified = emailVerified;
        this.photoUrl = photoUrl;
        this.countryId = countryId;
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.setupCompletedAt = setupCompletedAt;
    }

    /**
     * Someone signing in for the first time: a USER with the starting name they were given, who has not done the
     * first setup yet. No id yet.
     */
    public static User register(long publicId, String firebaseUid, String startingName, String email,
                                boolean emailVerified, String photoUrl, Instant now) {
        return new User(null, publicId, firebaseUid, startingName, email, emailVerified, photoUrl, null, Role.USER,
                now, null);
    }

    /** Rebuilds a stored user. For persistence adapters only: it does not check the username rules again. */
    public static User restore(Long id, long publicId, String firebaseUid, String username, String email,
                               boolean emailVerified, String photoUrl, Integer countryId, Role role,
                               Instant createdAt, Instant setupCompletedAt) {
        return new User(id, publicId, firebaseUid, username, email, emailVerified, photoUrl, countryId, role,
                createdAt, setupCompletedAt);
    }

    // ------------------------------------------------------------------ behaviour

    /**
     * Changes the public display name.
     *
     * @return true if it changed, so the caller knows it has to save
     * @throws InvalidUsernameException if it breaks the rules in Usernames; the current name is kept
     */
    public boolean changeUsername(String requested) {
        String valid = Usernames.validate(requested);
        if (valid.equals(username)) {
            return false;
        }
        this.username = valid;
        return true;
    }

    /**
     * Copies what the sign-in account says now: e-mail, whether it is verified, photo.
     *
     * @return true if something changed, so the caller knows it has to save
     */
    public boolean syncSignIn(String newEmail, boolean newEmailVerified, String newPhotoUrl) {
        if (Objects.equals(email, newEmail) && emailVerified == newEmailVerified
                && Objects.equals(photoUrl, newPhotoUrl)) {
            return false;
        }
        this.email = newEmail;
        this.emailVerified = newEmailVerified;
        this.photoUrl = newPhotoUrl;
        return true;
    }

    /** Sets the country; null clears it (the user is then left out of country statistics). */
    public void changeCountry(Integer newCountryId) {
        this.countryId = newCountryId;
    }

    /**
     * Records that the first-setup screen is done (saved or skipped). Only the first time counts.
     *
     * @return true if this is the first time, so the caller knows it has to save
     */
    public boolean completeSetup(Instant now) {
        if (setupCompletedAt != null) {
            return false;
        }
        this.setupCompletedAt = Objects.requireNonNull(now, "now");
        return true;
    }

    /** @return true if it changed. Who may change roles is decided by the use case. */
    public boolean changeRole(Role newRole) {
        Objects.requireNonNull(newRole, "role");
        if (role == newRole) {
            return false;
        }
        this.role = newRole;
        return true;
    }

    // ------------------------------------------------------------------ reading

    /** null until the user is stored. Never shown outside the backend. */
    public Long id() {
        return id;
    }

    public long publicId() {
        return publicId;
    }

    public String firebaseUid() {
        return firebaseUid;
    }

    public String username() {
        return username;
    }

    /** Private. */
    public String email() {
        return email;
    }

    public boolean emailVerified() {
        return emailVerified;
    }

    /** Private. */
    public String photoUrl() {
        return photoUrl;
    }

    public Integer countryId() {
        return countryId;
    }

    public Role role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** When the first-setup screen was done (saved or skipped); null if it has not been yet. */
    public Instant setupCompletedAt() {
        return setupCompletedAt;
    }

    public boolean setupCompleted() {
        return setupCompletedAt != null;
    }

    /** Two users are the same person when they have the same Firebase identity. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof User user && firebaseUid.equals(user.firebaseUid);
    }

    @Override
    public int hashCode() {
        return firebaseUid.hashCode();
    }

    /** No e-mail, photo or Firebase UID: they are private and must not reach the logs. */
    @Override
    public String toString() {
        return "User{id=" + id + ", publicId=" + publicId + ", username=" + username + ", role=" + role
                + ", countryId=" + countryId + "}";
    }
}
