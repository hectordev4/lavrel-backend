package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.UserNotFoundException;

/**
 * The one-time "Make it yours" screen (name, avatar, country) is done, saved or skipped. Remembered on the user, so
 * every device knows. Name, avatar and country are saved through their own endpoints; this only records the screen.
 */
public interface CompleteSetupUseCase {

    /**
     * Safe to repeat: if it is already done, the first time is kept and nothing changes.
     *
     * @throws UserNotFoundException if the user does not exist
     */
    void completeSetup(long userId);
}
