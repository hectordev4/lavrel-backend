package com.lavrel.assistant.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.assistant.domain.Guide;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Saved guides keep their sources as JSON in assistant_guide.sources; guides saved before 0.17.0 have fewer fields. */
class GuideSourcesJsonTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void sourcesSavedBeforeTheWikiNameStillRead() {
        Guide.Source[] old = mapper.readValue(
                "[{\"title\":\"Weapons\",\"url\":\"https://hades.fandom.com/wiki/Weapons\",\"license\":\"CC BY-SA 3.0\"}]",
                Guide.Source[].class);

        assertThat(old).containsExactly(new Guide.Source("Weapons", "https://hades.fandom.com/wiki/Weapons",
                null, "CC BY-SA 3.0", null));
    }

    @Test
    void newSourcesKeepTheWikiNameAndLicenseLink() {
        Guide.Source source = new Guide.Source("Weapons", "https://hades.fandom.com/wiki/Weapons", "Hades Wiki",
                "CC BY-SA 3.0", "https://creativecommons.org/licenses/by-sa/3.0/");

        Guide.Source[] back = mapper.readValue(mapper.writeValueAsString(new Guide.Source[] {source}),
                Guide.Source[].class);

        assertThat(back).containsExactly(source);
    }
}
