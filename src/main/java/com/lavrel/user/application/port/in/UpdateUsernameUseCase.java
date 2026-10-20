package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.InvalidUsernameException;
import com.lavrel.user.domain.UserNotFoundException;
import com.lavrel.user.domain.UsernameTakenException;

public interface UpdateUsernameUseCase {

    /**
     * Sets the public username. Also used by the sign-up form of our own accounts: right after creating the
     * account the frontend sends the username the person typed.
     *
     * @throws InvalidUsernameException if it is not 3-20 letters, digits or underscores, or is reserved
     * @throws UsernameTakenException   if somebody else has it (case-insensitive)
     * @throws UserNotFoundException    if the user does not exist
     */
    void updateUsername(long userId, String username);
}
