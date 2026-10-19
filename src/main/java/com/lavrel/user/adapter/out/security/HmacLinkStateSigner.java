package com.lavrel.user.adapter.out.security;

import com.lavrel.user.application.port.out.LinkStateSignerPort;
import com.lavrel.user.domain.InvalidIdentityProofException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * State format: "{userId}.{expiryEpochSeconds}.{HMAC-SHA256 signature}". Nobody without the
 * secret can forge a state for another user, and it stops working after the TTL (default 10 minutes).
 */
@Component
public class HmacLinkStateSigner implements LinkStateSignerPort {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final Duration ttl;
    private final Clock clock;

    public HmacLinkStateSigner(@Value("${app.steam.state-secret}") String secret,
                               @Value("${app.steam.state-ttl:PT10M}") Duration ttl,
                               Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "app.steam.state-secret must be at least 32 characters. Generate one with: openssl rand -hex 32");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public String sign(long userId) {
        long expiry = clock.instant().plus(ttl).getEpochSecond();
        String payload = userId + "." + expiry;
        return payload + "." + signature(payload);
    }

    @Override
    public long verify(String state) {
        if (state == null || state.isBlank()) {
            throw new InvalidIdentityProofException("Missing link state");
        }
        String[] parts = state.split("\\.");
        if (parts.length != 3) {
            throw new InvalidIdentityProofException("Invalid link state");
        }
        String expected = signature(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidIdentityProofException("Invalid link state");
        }
        try {
            if (clock.instant().getEpochSecond() > Long.parseLong(parts[1])) {
                throw new InvalidIdentityProofException("The Steam link request expired. Start again.");
            }
            return Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            throw new InvalidIdentityProofException("Invalid link state");
        }
    }

    private String signature(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not compute the link state signature", e);
        }
    }
}
