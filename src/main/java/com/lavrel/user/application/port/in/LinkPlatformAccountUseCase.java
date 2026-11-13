package com.lavrel.user.application.port.in;

import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformNotSupportedException;
import com.lavrel.user.domain.AccountLinkedElsewhereException;
import com.lavrel.user.domain.InvalidIdentityProofException;
import com.lavrel.user.domain.PlatformAccount;
import java.util.Map;

public interface LinkPlatformAccountUseCase {

    /**
     * Checks the signed state (who is linking), asks the platform to confirm the sign-in and links the
     * platform account to that user.
     *
     * @throws InvalidIdentityProofException  if the state is invalid or the platform does not confirm the sign-in
     * Safe to repeat: if the account is already linked to this user, it is returned unchanged.
     *
     * @throws AccountLinkedElsewhereException if the platform account is linked to another user
     * @throws PlatformNotSupportedException  if linking is not available for the platform yet
     */
    PlatformAccount link(Command command);

    /** callbackParams: every query parameter the platform sent back, including our state. */
    record Command(Platform platform, String state, Map<String, String> callbackParams) {
    }
}
