package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.UserNotFoundException;

public interface CreateSteamLinkUrlUseCase {

    /**
     * @return the Steam login URL the user's browser must be sent to
     * @throws UserNotFoundException if the user does not exist
     */
    String createLoginUrl(long userId);
}
