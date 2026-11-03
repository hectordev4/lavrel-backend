package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase;
import com.lavrel.catalog.application.port.out.FilterOptionsQueryPort;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BrowseFiltersService implements BrowseFiltersUseCase {

    static final int LIMIT = 30;

    private final FilterOptionsQueryPort options;

    public BrowseFiltersService(FilterOptionsQueryPort options) {
        this.options = options;
    }

    @Override
    public List<TagOption> tags(String query) {
        return options.tags(blankToNull(query), LIMIT);
    }

    @Override
    public List<CreatorOption> creators(CreatorRole role, String query) {
        return options.creators(role, blankToNull(query), LIMIT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
