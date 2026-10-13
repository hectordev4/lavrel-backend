package user.application.port.out;

import java.util.Map;
import user.domain.InvalidIdentityProofException;

/** Implemented in platform.steam. Validates the OpenID assertion against Steam. */
public interface SteamIdentityPort {

    /** @throws InvalidIdentityProofException if Steam does not confirm the assertion */
    SteamIdentity verifyOpenId(Map<String, String> openIdParams);

    record SteamIdentity(String steamId64, String personaName) {
    }
}
