package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.EmailAlreadyVerifiedException;
import com.lavrel.user.domain.EmailVerificationNotAllowedException;
import com.lavrel.user.domain.IdentityProviderUnavailableException;

/**
 * DEVELOPMENT ONLY: marks the caller's own e-mail as verified, for test accounts whose address cannot receive
 * the verification link. Exists only while app.dev.verify-email-uids is set.
 */
public interface ForceVerifyEmailUseCase {

    /**
     * @param alreadyVerified the email_verified flag of the caller's current token
     * @throws EmailVerificationNotAllowedException if the account is not on the allowlist
     * @throws EmailAlreadyVerifiedException        if the e-mail is already verified (each account uses it once)
     * @throws IdentityProviderUnavailableException if Firebase fails
     */
    void verifyOwnEmail(String firebaseUid, boolean alreadyVerified);
}
