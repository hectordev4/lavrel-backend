package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class UsernameGeneratorTest {

    @Test
    void picksOneAdjectiveAndOneNoun() {
        RandomGenerator random = mock(RandomGenerator.class);
        when(random.nextInt(org.mockito.ArgumentMatchers.anyInt())).thenReturn(0, 0);

        assertThat(new UsernameGenerator(random).generate()).isEqualTo("SwiftOtter");
    }
}
