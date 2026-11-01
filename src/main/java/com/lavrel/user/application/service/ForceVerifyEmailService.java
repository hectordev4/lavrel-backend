package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.ForceVerifyEmailUseCase;
import com.lavrel.user.application.port.out.IdentityProviderAdminPort;
import com.lavrel.user.domain.EmailAlreadyVerifiedException;
import com.lavrel.user.domain.EmailVerificationNotAllowedException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * The three locks of the forced verification:
 *  1. this bean (and its controller) only exists while app.dev.verify-email-uids is set;
 *  2. only the Firebase UIDs in that list, and only on the caller's own account (the uid comes from the token);
 *  3. refused once the e-mail is verified, so each account uses it once.
 * Every use is logged.
 */
@Service
@ConditionalOnProperty(name = "app.dev.verify-email-uids")
public class ForceVerifyEmailService implements ForceVerifyEmailUseCase {

    private static final Logger log = LoggerFactory.getLogger(ForceVerifyEmailService.class);

    private final IdentityProviderAdminPort identityProvider;
    private final Set<String> allowedUids;

    @Autowired
    public ForceVerifyEmailService(IdentityProviderAdminPort identityProvider,
                                   @Value("${app.dev.verify-email-uids}") List<String> allowedUids) {
        this(identityProvider, (Collection<String>) allowedUids);
        log.warn("DEV: forced e-mail verification is enabled for {} account(s). Remove app.dev.verify-email-uids "
                + "when done.", this.allowedUids.size());
    }

    /** For tests. */
    ForceVerifyEmailService(IdentityProviderAdminPort identityProvider, Collection<String> allowedUids) {
        this.identityProvider = identityProvider;
        this.allowedUids = allowedUids.stream().map(String::trim).filter(uid -> !uid.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void verifyOwnEmail(String firebaseUid, boolean alreadyVerified) {
        if (!allowedUids.contains(firebaseUid)) {
            throw new EmailVerificationNotAllowedException();
        }
        if (alreadyVerified) {
            throw new EmailAlreadyVerifiedException();
        }
        identityProvider.markEmailVerified(firebaseUid);
        log.warn("DEV: marked the e-mail of {} as verified", firebaseUid);
    }
}
