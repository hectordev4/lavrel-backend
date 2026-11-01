package com.lavrel.user.application.port.out;

import com.lavrel.user.domain.InvalidTokenException;

/** Implemented by the Firebase adapter. */
public interface TokenVerifierPort {

    /** @throws InvalidTokenException if the token is invalid or expired */
    VerifiedToken verify(String idToken);

    /**
     * emailVerified is Firebase's email_verified claim: always true for Google sign-in, true for e-mail and
     * password accounts only after the person clicked the verification link (and the token was refreshed).
     */
    record VerifiedToken(String uid, String email, boolean emailVerified, String name, String photoUrl) {
    }
}
