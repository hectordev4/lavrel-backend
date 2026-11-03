package com.lavrel.catalog.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorRole;
import com.lavrel.catalog.application.port.out.FilterOptionsQueryPort;
import org.junit.jupiter.api.Test;

class BrowseFiltersServiceTest {

    private final FilterOptionsQueryPort options = mock(FilterOptionsQueryPort.class);
    private final BrowseFiltersService service = new BrowseFiltersService(options);

    @Test
    void blankQueriesMeanNoTextFilter() {
        service.tags("   ");
        verify(options).tags(null, BrowseFiltersService.LIMIT);
    }

    @Test
    void creatorQueriesAreTrimmed() {
        service.creators(CreatorRole.DEVELOPER, " fromsoft ");
        verify(options).creators(CreatorRole.DEVELOPER, "fromsoft", BrowseFiltersService.LIMIT);
    }
}
