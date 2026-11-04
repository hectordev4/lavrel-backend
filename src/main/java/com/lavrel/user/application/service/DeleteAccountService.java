package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.DeleteAccountUseCase;
import com.lavrel.user.application.port.out.IdentityProviderAdminPort;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.LastAdminException;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import com.lavrel.user.domain.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Our data first, then Firebase: if Firebase fails, the user's data is already gone, and signing in again (which
 * creates an empty user) and repeating the call removes the sign-in account too. No transaction around both,
 * because Firebase is a network call.
 */
@Service
public class DeleteAccountService implements DeleteAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteAccountService.class);

    private final UserRepositoryPort users;
    private final IdentityProviderAdminPort identityProvider;

    public DeleteAccountService(UserRepositoryPort users, IdentityProviderAdminPort identityProvider) {
        this.users = users;
        this.identityProvider = identityProvider;
    }

    @Override
    public void deleteAccount(long userId) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.role() == Role.ADMIN && users.countByRole(Role.ADMIN) <= 1) {
            throw new LastAdminException();
        }
        users.delete(userId);
        log.info("Deleted the data of user {} (public id {})", userId, user.publicId());
        identityProvider.deleteUser(user.firebaseUid());
        log.info("Deleted the sign-in account of public id {}", user.publicId());
    }
}
