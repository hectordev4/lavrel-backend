package com.lavrel.user.domain;

public class CountryNotFoundException extends RuntimeException {
    public CountryNotFoundException(int countryId) {
        super("Country not found: " + countryId);
    }
}
