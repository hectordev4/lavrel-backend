package com.lavrel.catalog.adapter.out.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.lavrel.shared.storage.SupabaseStorage;
import org.junit.jupiter.api.Test;

class SteamIconFilesTest {

    private final SteamIconFiles files = new SteamIconFiles(mock(SupabaseStorage.class), "achievement-icons",
            "https://shared.fastly.steamstatic.com/community_assets/images/apps/");

    @Test
    void onlySteamIconPathsAreEverDownloaded() {
        assertThat(SteamIconFiles.ICON_PATH.matcher("2950790/dbc5ae8efb95148ed8466cbec7d23be7b3f0ac4c.jpg").matches())
                .isTrue();
        assertThat(files.downloadFromSteam("../../etc/passwd")).isEmpty();
        assertThat(files.downloadFromSteam("2950790/x.svg")).isEmpty();
        assertThat(files.downloadFromSteam("http://evil/1.jpg")).isEmpty();
        assertThat(files.downloadFromSteam(null)).isEmpty();
    }
}
