package com.lavrel.user.application.port.out;

import com.lavrel.user.domain.InvalidIdentityProofException;

/**
 * Produces and checks the short-lived "state" that travels through Steam's login redirect.
 * Steam sends the browser back without our auth token, so the state is what says which user is linking.
 */
public interface LinkStateSignerPort {

    String sign(long userId);

    /**
     * @return the user the state was issued for
     * @throws InvalidIdentityProofException if the state is missing, tampered with or expired
     */
    long verify(String state);
}
