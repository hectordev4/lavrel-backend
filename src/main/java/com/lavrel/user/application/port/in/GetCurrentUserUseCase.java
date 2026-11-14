package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.UserNotFoundException;

public interface GetCurrentUserUseCase {

    /**
     * The user's own profile. email and photoUrl come from the sign-in account and are PRIVATE:
     * they are only ever returned to the user themself, never to anybody else.
     *
     * @throws UserNotFoundException if the user does not exist
     */
    CurrentUserView getCurrentUser(long userId);

    /**
     * publicId is what other people see; the database id never leaves the backend. emailVerified is the stored
     * flag, refreshed from the token earlier in the same request, so it is current. setupCompleted is false until
     * the user saves or skips the first-setup screen.
     */
    record CurrentUserView(long publicId, String firebaseUid, String username, String email, boolean emailVerified,
                           String photoUrl, Integer countryId, Role role, boolean setupCompleted) {
    }
}
