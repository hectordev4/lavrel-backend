package com.lavrel.platform.adapter.out.steam;

import com.lavrel.platform.adapter.out.steam.SteamHttp.Response;
import com.lavrel.shared.domain.PlatformUnavailableException;
import com.lavrel.user.application.port.out.SteamIdentityPort;
import com.lavrel.user.domain.InvalidIdentityProofException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Steam OpenID 2.0: builds the login URL and verifies the assertion Steam sends back.
 * A login is only accepted when Steam itself confirms it (check_authentication), the assertion
 * comes from Steam's endpoint, and it was issued for OUR callback URL.
 */
@Component
public class SteamOpenIdVerifier implements SteamIdentityPort {

    private static final String OPENID_ENDPOINT = "https://steamcommunity.com/openid/login";
    private static final String OPENID_NS = "http://specs.openid.net/auth/2.0";
    private static final String IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select";
    private static final Pattern CLAIMED_ID = Pattern.compile("^https://steamcommunity\\.com/openid/id/(\\d{17})$");

    private final SteamHttp http;
    private final String callbackUrl;

    public SteamOpenIdVerifier(SteamHttp http,
                               @Value("${app.steam.callback-url}") String callbackUrl) {
        this.http = http;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public String loginUrl(String state) {
        URI callback = URI.create(callbackUrl);
        String realm = callback.getScheme() + "://" + callback.getAuthority();
        String returnTo = callbackUrl + (callbackUrl.contains("?") ? "&" : "?") + "state=" + encode(state);

        return OPENID_ENDPOINT + "?"
                + param("openid.ns", OPENID_NS) + "&"
                + param("openid.mode", "checkid_setup") + "&"
                + param("openid.return_to", returnTo) + "&"
                + param("openid.realm", realm) + "&"
                + param("openid.identity", IDENTIFIER_SELECT) + "&"
                + param("openid.claimed_id", IDENTIFIER_SELECT);
    }

    @Override
    public SteamIdentity verifyOpenId(Map<String, String> params) {
        require("id_res".equals(params.get("openid.mode")), "Unexpected OpenID response");
        require(OPENID_ENDPOINT.equals(params.get("openid.op_endpoint")), "The login did not come from Steam");
        String returnTo = params.get("openid.return_to");
        require(returnTo != null && returnTo.startsWith(callbackUrl), "The login was issued for another site");

        Matcher claimedId = CLAIMED_ID.matcher(String.valueOf(params.get("openid.claimed_id")));
        require(claimedId.matches(), "The Steam account ID is missing or malformed");

        // Ask Steam to confirm its own signature: same openid.* fields, with the mode changed.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith("openid.")) {
                form.add(key, value);
            }
        });
        form.set("openid.mode", "check_authentication");
        Response confirmation = http.postForm(OPENID_ENDPOINT, form);
        require(confirmation.ok() && confirmation.body() != null && confirmation.body().contains("is_valid:true"),
                "Steam did not confirm the login");

        String steamId64 = claimedId.group(1);
        return new SteamIdentity(steamId64, personaName(steamId64));
    }

    /** The display name is a nicety: a failure here must not block linking the account. */
    private String personaName(String steamId64) {
        try {
            Response response = http.get("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/"
                    + "?key={key}&steamids={steamId}", http.apiKey(), steamId64);
            if (!response.ok()) {
                return null;
            }
            Map<String, Object> body = SteamJson.map(http.json(response).get("response"));
            List<Object> players = SteamJson.list(body.get("players"));
            return players.isEmpty() ? null : SteamJson.string(SteamJson.map(players.get(0)).get("personaname"));
        } catch (PlatformUnavailableException e) {
            return null;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidIdentityProofException(message);
        }
    }

    private static String param(String key, String value) {
        return key + "=" + encode(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
