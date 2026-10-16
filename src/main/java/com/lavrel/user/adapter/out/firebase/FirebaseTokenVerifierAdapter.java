package com.lavrel.user.adapter.out.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.lavrel.user.application.port.out.TokenVerifierPort;
import com.lavrel.user.domain.InvalidTokenException;
import org.springframework.stereotype.Component;

/** Outbound adapter: verifies a Firebase ID token and translates it to the app's VerifiedToken. */
@Component
public class FirebaseTokenVerifierAdapter implements TokenVerifierPort {

    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenVerifierAdapter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public VerifiedToken verify(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            return new VerifiedToken(token.getUid(), token.getEmail(), token.getName(), token.getPicture());
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid Firebase ID token", e);
        }
    }
}
