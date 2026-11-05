package com.lavrel.shared.web;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory token buckets, one per key ("ip:1.2.3.4", "user:42"...). Each bucket holds up to {@code capacity}
 * requests and refills evenly over {@code period}. Kept in memory on purpose: there is one instance on Render, and a
 * restart only forgets who was close to the limit. Idle buckets are dropped regularly so memory stays small.
 */
public final class RateLimiter {

    private static final Duration IDLE = Duration.ofMinutes(15);
    private static final int CLEANUP_EVERY = 1_000;

    private final int capacity;
    private final double tokensPerNano;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();

    public RateLimiter(int capacity, Duration period, Clock clock) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        this.capacity = capacity;
        this.tokensPerNano = capacity / (double) period.toNanos();
        this.clock = clock;
    }

    /** @return 0 if the request may go ahead, otherwise how many seconds to wait (at least 1) */
    public long tryAcquire(String key) {
        long now = nanos();
        if (calls.incrementAndGet() % CLEANUP_EVERY == 0) {
            dropIdle(now);
        }
        return buckets.computeIfAbsent(key, k -> new Bucket(capacity, now)).take(now);
    }

    int trackedKeys() {
        return buckets.size();
    }

    private void dropIdle(long now) {
        long idle = IDLE.toNanos();
        for (Iterator<Bucket> it = buckets.values().iterator(); it.hasNext(); ) {
            if (now - it.next().lastSeen > idle) {
                it.remove();
            }
        }
    }

    private long nanos() {
        java.time.Instant i = clock.instant();
        return i.getEpochSecond() * 1_000_000_000L + i.getNano();
    }

    private final class Bucket {

        private double tokens;
        private long last;
        private volatile long lastSeen;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.last = now;
            this.lastSeen = now;
        }

        synchronized long take(long now) {
            tokens = Math.min(capacity, tokens + (now - last) * tokensPerNano);
            last = now;
            lastSeen = now;
            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }
            double missing = 1 - tokens;
            return Math.max(1, (long) Math.ceil(missing / tokensPerNano / 1_000_000_000d));
        }
    }
}
