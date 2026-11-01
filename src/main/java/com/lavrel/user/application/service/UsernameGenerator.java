package com.lavrel.user.application.service;

import com.lavrel.user.domain.FriendlyNames;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gives new users their starting name ("SwiftOtter"). It owns the random source, so the domain stays
 * deterministic; swapping the naming scheme means changing this class (or FriendlyNames) only.
 */
@Component
public class UsernameGenerator {

    private final RandomGenerator random;

    @Autowired
    public UsernameGenerator() {
        this(new SecureRandom());
    }

    /** For tests: a fixed random source gives a known name. */
    UsernameGenerator(RandomGenerator random) {
        this.random = random;
    }

    public String generate() {
        return FriendlyNames.compose(random);
    }
}
