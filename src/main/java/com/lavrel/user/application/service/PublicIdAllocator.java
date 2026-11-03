package com.lavrel.user.application.service;

import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.PublicIds;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Hands out a public id nobody has yet: draws a random 16-digit number and draws again if it is taken.
 * With 9e15 possible ids a collision is practically impossible, but it is checked anyway; the unique
 * constraint on users.public_id is the final guard. Any place that creates users asks this class.
 */
@Component
public class PublicIdAllocator {

    static final int MAX_ATTEMPTS = 5;

    private final UserRepositoryPort users;
    private final RandomGenerator random;

    @Autowired
    public PublicIdAllocator(UserRepositoryPort users) {
        this(users, new SecureRandom());
    }

    /** For tests: a fixed random source makes the collision case deterministic. */
    PublicIdAllocator(UserRepositoryPort users, RandomGenerator random) {
        this.users = users;
        this.random = random;
    }

    /** @throws IllegalStateException if MAX_ATTEMPTS draws in a row were taken (it cannot realistically happen) */
    public long allocate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            long candidate = PublicIds.draw(random);
            if (!users.existsByPublicId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not find a free public id in " + MAX_ATTEMPTS + " attempts");
    }
}
