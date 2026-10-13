package user.application.port.in;

import java.util.Map;
import user.domain.AccountAlreadyLinkedException;
import user.domain.InvalidIdentityProofException;
import user.domain.PlatformAccount;

public interface LinkSteamAccountUseCase {

    /**
     * Verifies the Steam OpenID assertion and links the SteamID64 to the user.
     *
     * @throws InvalidIdentityProofException  if Steam does not confirm the assertion
     * @throws AccountAlreadyLinkedException  if the Steam account is already linked (any user)
     */
    PlatformAccount link(Command command);

    record Command(long userId, Map<String, String> openIdParams) {
    }
}
