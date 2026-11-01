package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.PublicIds;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class PublicIdAllocatorTest {

    private static final long FIRST = PublicIds.MIN + 1;
    private static final long SECOND = PublicIds.MIN + 2;

    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final RandomGenerator random = mock(RandomGenerator.class);
    private final PublicIdAllocator allocator = new PublicIdAllocator(users, random);

    @Test
    void freeCandidateIsUsedStraightAway() {
        when(random.nextLong(PublicIds.MIN, PublicIds.MAX + 1)).thenReturn(FIRST);
        when(users.existsByPublicId(FIRST)).thenReturn(false);

        assertThat(allocator.allocate()).isEqualTo(FIRST);
    }

    @Test
    void takenCandidateIsDrawnAgain() {
        when(random.nextLong(PublicIds.MIN, PublicIds.MAX + 1)).thenReturn(FIRST, SECOND);
        when(users.existsByPublicId(FIRST)).thenReturn(true);
        when(users.existsByPublicId(SECOND)).thenReturn(false);

        assertThat(allocator.allocate()).isEqualTo(SECOND);
    }

    @Test
    void givesUpAfterTheMaximumAttempts() {
        when(random.nextLong(PublicIds.MIN, PublicIds.MAX + 1)).thenReturn(FIRST);
        when(users.existsByPublicId(anyLong())).thenReturn(true);

        assertThatThrownBy(allocator::allocate).isInstanceOf(IllegalStateException.class);
    }
}
