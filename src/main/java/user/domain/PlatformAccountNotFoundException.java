package user.domain;

public class PlatformAccountNotFoundException extends RuntimeException {
    public PlatformAccountNotFoundException(long platformAccountId) {
        super("Platform account not found: " + platformAccountId);
    }
}
