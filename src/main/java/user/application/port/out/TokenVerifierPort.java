package user.application.port.out;

import user.domain.InvalidTokenException;

/** Implemented by the Firebase adapter. */
public interface TokenVerifierPort {

    /** @throws InvalidTokenException if the token is invalid or expired */
    VerifiedToken verify(String idToken);

    record VerifiedToken(String uid, String email, String name, String photoUrl) {
    }
}
