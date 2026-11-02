package com.lavrel.user.adapter.in.web;

import com.lavrel.user.application.port.in.ListCountriesUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reference data for the country picker. */
@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final ListCountriesUseCase listCountries;

    public CountryController(ListCountriesUseCase listCountries) {
        this.listCountries = listCountries;
    }

    @GetMapping
    public CountriesResponse listCountries() {
        return new CountriesResponse(listCountries.listCountries().stream()
                .map(c -> new CountryResponse(c.id(), c.code(), c.name()))
                .toList());
    }

    /** Wrapped in an object so fields can be added later without breaking clients. */
    public record CountriesResponse(List<CountryResponse> countries) {
    }

    public record CountryResponse(int id, String code, String name) {
    }
}
