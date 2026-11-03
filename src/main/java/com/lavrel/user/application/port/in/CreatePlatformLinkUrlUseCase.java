package com.lavrel.user.application.port.in;

import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformNotSupportedException;
import com.lavrel.user.domain.UserNotFoundException;

public interface CreatePlatformLinkUrlUseCase {

    /**
     * @return the platform sign-in URL the user's browser must be sent to
     * @throws UserNotFoundException         if the user does not exist
     * @throws PlatformNotSupportedException if linking is not available for the platform yet
     */
    String createLoginUrl(long userId, Platform platform);
}
