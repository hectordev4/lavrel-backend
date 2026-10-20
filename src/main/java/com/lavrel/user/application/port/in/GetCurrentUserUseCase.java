package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.UserNotFoundException;

public interface GetCurrentUserUseCase {

    /**
     * The user's own profile. email and photoUrl come from the linked Google account and are PRIVATE:
     * they are only ever returned to the user themself, never to anybody else.
     *
     * @throws UserNotFoundException if the user does not exist
     */
    CurrentUserView getCurrentUser(long userId);

    record CurrentUserView(long userId, String firebaseUid, String username, String email, String photoUrl,
                           Integer countryId) {
    }
}
