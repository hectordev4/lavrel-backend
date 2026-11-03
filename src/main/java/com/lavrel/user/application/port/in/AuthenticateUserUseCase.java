package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.InvalidTokenException;
import com.lavrel.user.domain.Role;

public interface AuthenticateUserUseCase {

    /**
     * Verifies the Firebase ID token and returns the matching user,
     * creating it on first login (role USER, random username, random public id).
     *
     * @throws InvalidTokenException if the token is invalid or expired
     */
    AuthenticatedUser authenticate(String firebaseIdToken);

    /**
     * What the security layer needs: ids, the role and whether the e-mail is verified; never the domain object.
     * Unverified accounts may only read their own profile (see SecurityConfig).
     */
    record AuthenticatedUser(long userId, String firebaseUid, Role role, boolean emailVerified) {
    }
}
