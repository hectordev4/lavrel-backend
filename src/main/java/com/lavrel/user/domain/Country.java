package com.lavrel.user.domain;

import java.util.Objects;

/** APP. Seeded from the ISO 3166-1 list. */
public record Country(Integer id, String code, String name) {
    public Country {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        if (code.length() != 2) {
            throw new IllegalArgumentException("code must be ISO 3166-1 alpha-2");
        }
        code = code.toUpperCase();
    }
}
