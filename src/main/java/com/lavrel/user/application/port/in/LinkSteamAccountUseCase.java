package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.InvalidIdentityProofException;
import com.lavrel.user.domain.PlatformAccount;
import java.util.Map;

public interface LinkSteamAccountUseCase {

    /**
     * Checks the signed state (who is linking), verifies the Steam OpenID assertion and links the
     * SteamID64 to that user.
     *
     * @throws InvalidIdentityProofException  if the state is invalid or Steam does not confirm the assertion
     * @throws AccountAlreadyLinkedException  if the Steam account is already linked (any user)
     */
    PlatformAccount link(Command command);

    record Command(String state, Map<String, String> openIdParams) {
    }
}
