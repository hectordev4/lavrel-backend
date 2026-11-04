package com.lavrel.user.adapter.out.firebase;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.lavrel.user.application.port.out.IdentityProviderAdminPort;
import com.lavrel.user.domain.IdentityProviderUnavailableException;
import org.springframework.stereotype.Component;

/** Firebase implementation of IdentityProviderAdminPort, through the Admin SDK (service-account credentials). */
@Component
public class FirebaseIdentityAdminAdapter implements IdentityProviderAdminPort {

    private final FirebaseAuth firebaseAuth;

    public FirebaseIdentityAdminAdapter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public void markEmailVerified(String firebaseUid) {
        try {
            firebaseAuth.updateUser(new UserRecord.UpdateRequest(firebaseUid).setEmailVerified(true));
        } catch (FirebaseAuthException e) {
            throw new IdentityProviderUnavailableException("Firebase refused to mark the e-mail as verified", e);
        }
    }

    @Override
    public void deleteUser(String firebaseUid) {
        try {
            firebaseAuth.deleteUser(firebaseUid);
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                return;
            }
            throw new IdentityProviderUnavailableException("Firebase refused to delete the account", e);
        }
    }
}
