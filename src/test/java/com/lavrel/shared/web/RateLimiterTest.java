package com.lavrel.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    /** A clock the test moves by hand. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-05T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void allowsTheCapacityThenAsksToWait() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), clock);

        assertThat(limiter.tryAcquire("ip:1")).isZero();
        assertThat(limiter.tryAcquire("ip:1")).isZero();
        assertThat(limiter.tryAcquire("ip:1")).isZero();
        assertThat(limiter.tryAcquire("ip:1")).isBetween(1L, 20L);
    }

    @Test
    void refillsOverTime() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(1), clock);
        limiter.tryAcquire("u");
        limiter.tryAcquire("u");
        assertThat(limiter.tryAcquire("u")).isPositive();

        clock.advance(Duration.ofSeconds(31));

        assertThat(limiter.tryAcquire("u")).isZero();
    }

    @Test
    void keysAreIndependent() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), clock);

        assertThat(limiter.tryAcquire("user:1")).isZero();
        assertThat(limiter.tryAcquire("user:2")).isZero();
        assertThat(limiter.tryAcquire("user:1")).isPositive();
    }
}
