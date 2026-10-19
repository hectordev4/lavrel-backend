package com.lavrel.user.application.port.out;

import com.lavrel.user.domain.InvalidIdentityProofException;
import java.util.Map;

/** Implemented in platform.adapter.out.steam. Knows the Steam OpenID protocol. */
public interface SteamIdentityPort {

    /** The Steam login page URL. Steam sends the browser back to our callback with this state attached. */
    String loginUrl(String state);

    /** @throws InvalidIdentityProofException if Steam does not confirm the assertion */
    SteamIdentity verifyOpenId(Map<String, String> openIdParams);

    record SteamIdentity(String steamId64, String personaName) {
    }
}
