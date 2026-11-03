package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.lavrel.user.application.port.out.IdentityProviderAdminPort;
import com.lavrel.user.domain.EmailAlreadyVerifiedException;
import com.lavrel.user.domain.EmailVerificationNotAllowedException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForceVerifyEmailServiceTest {

    private final IdentityProviderAdminPort identityProvider = mock(IdentityProviderAdminPort.class);
    private final ForceVerifyEmailService service =
            new ForceVerifyEmailService(identityProvider, List.of(" test-uid ", ""));

    @Test
    void allowedUnverifiedAccountIsVerified() {
        service.verifyOwnEmail("test-uid", false);

        verify(identityProvider).markEmailVerified("test-uid");
    }

    @Test
    void accountNotOnTheAllowlistIsRefused() {
        assertThatThrownBy(() -> service.verifyOwnEmail("someone-else", false))
                .isInstanceOf(EmailVerificationNotAllowedException.class);
        verify(identityProvider, never()).markEmailVerified(anyString());
    }

    @Test
    void secondUseIsRefused() {
        assertThatThrownBy(() -> service.verifyOwnEmail("test-uid", true))
                .isInstanceOf(EmailAlreadyVerifiedException.class);
        verify(identityProvider, never()).markEmailVerified(anyString());
    }
}
