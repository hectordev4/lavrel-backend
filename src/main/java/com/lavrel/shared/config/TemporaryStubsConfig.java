package com.lavrel.shared.config;

import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.library.application.port.out.PlatformLibraryPort;
import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.out.SteamIdentityPort;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * TEMPORARY. Lets the app start before the real Steam adapters exist. Active only with the
 * "stubs" profile. Delete this file once SteamIdentityPort, PlatformLibraryPort and
 * PlatformCatalogPort have real implementations.
 * (TokenVerifierPort is no longer stubbed: FirebaseTokenVerifierAdapter implements it.)
 */
@Configuration
@Profile("stubs")
public class TemporaryStubsConfig {

    private static UnsupportedOperationException notYet(String what) {
        return new UnsupportedOperationException(what + " adapter is not implemented yet");
    }

    @Bean
    public SteamIdentityPort steamIdentityStub() {
        return openIdParams -> {
            throw notYet("Steam OpenID");
        };
    }

    @Bean
    public PlatformLibraryPort platformLibraryStub() {
        return new PlatformLibraryPort() {
            @Override
            public Platform platform() {
                return Platform.STEAM;
            }

            @Override
            public List<PlatformGame> fetchOwnedGames(String externalId) {
                throw notYet("Steam library");
            }

            @Override
            public List<PlatformUnlock> fetchUnlocks(String externalId, String platformGameId) {
                throw notYet("Steam library");
            }
        };
    }

    @Bean
    public PlatformCatalogPort platformCatalogStub() {
        return new PlatformCatalogPort() {
            @Override
            public Platform platform() {
                return Platform.STEAM;
            }

            @Override
            public Optional<PlatformGameInfo> fetchGameInfo(String platformGameId) {
                throw notYet("Steam catalog");
            }

            @Override
            public List<PlatformAchievement> fetchAchievements(String platformGameId) {
                throw notYet("Steam catalog");
            }
        };
    }
}
