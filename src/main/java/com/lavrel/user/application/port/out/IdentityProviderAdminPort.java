package com.lavrel.user.application.port.out;

import com.lavrel.user.domain.IdentityProviderUnavailableException;

/**
 * Administrative changes to accounts in the identity provider (Firebase today). Sign-in itself never goes
 * through here: that is TokenVerifierPort. Future admin actions (disable an account, revoke sessions...) belong
 * on this port too.
 */
public interface IdentityProviderAdminPort {

    /** @throws IdentityProviderUnavailableException if the provider fails or refuses the change */
    void markEmailVerified(String firebaseUid);
}
