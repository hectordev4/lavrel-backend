package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.InvalidUsernameException;
import com.lavrel.user.domain.UserNotFoundException;

public interface UpdateUsernameUseCase {

    /**
     * Sets the public username, a free display name that other users may also have. The sign-up form of
     * e-mail accounts calls this right after creating the account, with the name the person typed.
     *
     * @throws InvalidUsernameException if it breaks the rules in Usernames (length, characters, reserved)
     * @throws UserNotFoundException    if the user does not exist
     */
    void updateUsername(long userId, String username);
}
