package com.lavrel.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserNotFoundExceptionsTest {

    @Test
    void internalIdNeverReachesTheMessage() {
        UserNotFoundException e = new UserNotFoundException(42L);

        assertThat(e.getMessage()).isEqualTo("User not found").doesNotContain("42");
        assertThat(e.userId()).isEqualTo(42L);
    }

    @Test
    void publicIdMayBeShown() {
        long publicId = PublicIds.MIN + 42;

        assertThat(new PublicUserNotFoundException(publicId).getMessage()).contains(String.valueOf(publicId));
    }
}
