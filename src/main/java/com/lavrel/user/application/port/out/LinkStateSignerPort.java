package com.lavrel.user.application.port.out;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.domain.InvalidIdentityProofException;

/**
 * Produces and checks the short-lived "state" that travels through a platform's sign-in redirect.
 * The platform sends the browser back without our auth token, so the state is what says which user is linking.
 * The state is bound to one platform: a state issued for Steam is rejected on another platform's callback.
 */
public interface LinkStateSignerPort {

    String sign(long userId, Platform platform);

    /**
     * @return the user the state was issued for
     * @throws InvalidIdentityProofException if the state is missing, tampered with, expired or for another platform
     */
    long verify(String state, Platform platform);
}
