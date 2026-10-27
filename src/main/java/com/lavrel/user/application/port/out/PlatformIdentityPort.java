package com.lavrel.user.application.port.out;

import com.lavrel.shared.domain.PlatformBound;
import com.lavrel.user.domain.InvalidIdentityProofException;
import java.util.Map;

/**
 * Proves that a user owns a platform account. One implementation per platform: Steam uses OpenID 2.0, Xbox
 * would use Microsoft OAuth, PSN its own sign-in. Every flow has the same two steps: send the browser to the
 * platform with our signed state, then verify what the platform sends back to our callback.
 */
public interface PlatformIdentityPort extends PlatformBound {

    /** The platform's sign-in page; the platform sends the browser back to our callback with this state. */
    String loginUrl(String state);

    /**
     * @param callbackParams every query parameter the platform sent back to our callback
     * @throws InvalidIdentityProofException if the platform does not confirm the sign-in
     */
    PlatformIdentity verify(Map<String, String> callbackParams);

    /** externalId is the platform's account id (SteamID64, XUID...); displayName may be null. */
    record PlatformIdentity(String externalId, String displayName) {
    }
}
