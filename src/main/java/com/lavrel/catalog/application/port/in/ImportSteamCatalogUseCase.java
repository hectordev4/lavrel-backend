package com.lavrel.catalog.application.port.in;

public interface ImportSteamCatalogUseCase {

    /**
     * Creates (or refreshes) the canonical Game, its Steam GameMapping and the Achievement copy.
     *
     * @return the canonical game id
     */
    long importGame(String steamAppId);
}
