package com.lavrel.catalog.application.port.out;

import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorOption;
import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorRole;
import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.TagOption;
import java.util.List;

public interface FilterOptionsQueryPort {

    List<TagOption> tags(String query, int limit);

    List<CreatorOption> creators(CreatorRole role, String query, int limit);
}
