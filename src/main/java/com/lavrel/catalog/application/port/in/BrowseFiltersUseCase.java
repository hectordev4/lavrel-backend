package com.lavrel.catalog.application.port.in;

import java.util.List;

/** Options for the search filters (tags, franchises, developers, publishers), most used first. Public. */
public interface BrowseFiltersUseCase {

    /** query optional: names containing it, those starting with it first. */
    List<TagOption> tags(String query);

    List<CreatorOption> creators(CreatorRole role, String query);

    enum CreatorRole {
        FRANCHISE, DEVELOPER, PUBLISHER
    }

    /** gameCount: published games the tag really describes (among their top 10 tags). */
    record TagOption(long id, String name, int gameCount) {
    }

    record CreatorOption(long id, String name, CreatorRole role, int gameCount) {
    }
}
