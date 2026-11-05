package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.out.TokenVerifierPort;
import com.lavrel.user.application.port.out.TokenVerifierPort.VerifiedToken;
import com.lavrel.user.application.port.out.UserActivityPort;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.PublicIds;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AuthenticateUserServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC);

    private final TokenVerifierPort tokens = mock(TokenVerifierPort.class);
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final PublicIdAllocator publicIds = mock(PublicIdAllocator.class);
    private final UsernameGenerator usernames = mock(UsernameGenerator.class);
    private final UserActivityPort activity = mock(UserActivityPort.class);
    private final AuthenticateUserService service =
            new AuthenticateUserService(tokens, users, publicIds, usernames, activity, CLOCK);

    @Test
    void newUserIsAPlainUserWithAFriendlyNameAndAPublicId() {
        when(tokens.verify("t")).thenReturn(new VerifiedToken("uid-1", "a@b.c", false, "Real Name", null));
        when(users.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());
        when(publicIds.allocate()).thenReturn(PublicIds.MIN + 5);
        when(usernames.generate()).thenReturn("SwiftOtter");
        when(users.save(any())).thenAnswer(i -> withId(i.getArgument(0)));

        AuthenticatedUser result = service.authenticate("t");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        Mockito.verify(users).save(saved.capture());
        assertThat(saved.getValue().role()).isEqualTo(Role.USER);
        assertThat(saved.getValue().username()).isEqualTo("SwiftOtter");
        assertThat(saved.getValue().emailVerified()).isFalse();
        assertThat(saved.getValue().publicId()).isEqualTo(PublicIds.MIN + 5);
        assertThat(result.emailVerified()).isFalse();
    }

    @Test
    void roleAndVerificationReachTheSecurityLayer() {
        User admin = User.restore(7L, PublicIds.MIN, "uid-7", "boss", "a@b.c", true, null, null, Role.ADMIN,
                CLOCK.instant());
        when(tokens.verify("t")).thenReturn(new VerifiedToken("uid-7", "a@b.c", true, null, null));
        when(users.findByFirebaseUid("uid-7")).thenReturn(Optional.of(admin));

        AuthenticatedUser result = service.authenticate("t");

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        assertThat(result.emailVerified()).isTrue();
        assertThat(result.userId()).isEqualTo(7L);
    }

    @Test
    void clickingTheVerificationLinkIsStoredOnTheNextRequest() {
        User stored = User.restore(7L, PublicIds.MIN, "uid-7", "SwiftOtter", "a@b.c", false, null, null, Role.USER,
                CLOCK.instant());
        when(tokens.verify("t")).thenReturn(new VerifiedToken("uid-7", "a@b.c", true, null, null));
        when(users.findByFirebaseUid("uid-7")).thenReturn(Optional.of(stored));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        service.authenticate("t");

        Mockito.verify(users).save(stored);
        assertThat(stored.emailVerified()).isTrue();
    }

    @Test
    void unchangedSignInDataIsNotWritten() {
        User stored = User.restore(7L, PublicIds.MIN, "uid-7", "SwiftOtter", "a@b.c", true, null, null, Role.USER,
                CLOCK.instant());
        when(tokens.verify("t")).thenReturn(new VerifiedToken("uid-7", "a@b.c", true, null, null));
        when(users.findByFirebaseUid("uid-7")).thenReturn(Optional.of(stored));

        service.authenticate("t");

        Mockito.verify(users, Mockito.never()).save(any());
    }

    @Test
    void everyAuthenticationRecordsActivity() {
        User stored = User.restore(7L, PublicIds.MIN, "uid-7", "SwiftOtter", "a@b.c", true, null, null, Role.USER,
                CLOCK.instant());
        when(tokens.verify("t")).thenReturn(new VerifiedToken("uid-7", "a@b.c", true, null, null));
        when(users.findByFirebaseUid("uid-7")).thenReturn(Optional.of(stored));

        service.authenticate("t");

        Mockito.verify(activity).recordSeen(7L, CLOCK.instant());
    }

    private static User withId(User u) {
        return User.restore(1L, u.publicId(), u.firebaseUid(), u.username(), u.email(), u.emailVerified(), u.photoUrl(),
                u.countryId(), u.role(), u.createdAt());
    }
}
