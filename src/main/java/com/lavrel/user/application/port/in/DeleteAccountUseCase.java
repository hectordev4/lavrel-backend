package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.IdentityProviderUnavailableException;
import com.lavrel.user.domain.LastAdminException;
import com.lavrel.user.domain.UserNotFoundException;

/**
 * Deletes the user's account for good (the GDPR right to erasure): the user, linked accounts, library, unlocks,
 * tracking list and collections in our database, and the sign-in account in Firebase. It cannot be undone.
 */
public interface DeleteAccountUseCase {

    /**
     * @throws UserNotFoundException                if the user does not exist
     * @throws LastAdminException                   if the user is the only admin
     * @throws IdentityProviderUnavailableException if our data is gone but Firebase could not remove the sign-in
     *                                              account; signing in again and repeating the call finishes it
     */
    void deleteAccount(long userId);
}
