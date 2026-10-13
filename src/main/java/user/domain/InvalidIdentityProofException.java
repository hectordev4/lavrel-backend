package user.domain;

/** The platform (e.g. Steam OpenID) did not confirm that the user owns the account. Maps to HTTP 400/403. */
public class InvalidIdentityProofException extends RuntimeException {
    public InvalidIdentityProofException(String message) {
        super(message);
    }
}
