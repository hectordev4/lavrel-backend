package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformRegistry;
import java.util.List;

/** Picks the Steam catalog adapter out of every registered one. */
final class SteamCatalogPorts {

    private SteamCatalogPorts() {
    }

    static PlatformCatalogPort steam(List<PlatformCatalogPort> ports) {
        return new PlatformRegistry<>(ports, "catalog data").require(Platform.STEAM);
    }

    /** Small pause between Steam calls: the store API is rate limited. */
    static void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
