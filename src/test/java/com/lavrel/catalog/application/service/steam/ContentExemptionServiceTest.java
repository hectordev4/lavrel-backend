package com.lavrel.catalog.application.service.steam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.catalog.application.port.in.ManageContentExemptionsUseCase.ExemptionResult;
import com.lavrel.catalog.application.port.in.ManageContentExemptionsUseCase.Status;
import com.lavrel.catalog.application.port.out.ContentExemptionRepositoryPort;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.ImportFailureRepositoryPort;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.shared.domain.Platform;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentExemptionServiceTest {

    private final GameMappingRepositoryPort mappings = mock(GameMappingRepositoryPort.class);
    private final ImportFailureRepositoryPort failures = mock(ImportFailureRepositoryPort.class);
    private final ContentExemptionRepositoryPort exemptions = mock(ContentExemptionRepositoryPort.class);
    private final SteamImportRun importRun = mock(SteamImportRun.class);
    private final ContentExemptionService service =
            new ContentExemptionService(mappings, failures, exemptions, importRun, 0);

    @BeforeEach
    void setUp() {
        when(mappings.findByPlatformAndPlatformGameIds(eq(Platform.STEAM), any())).thenReturn(List.of());
    }

    private static SteamImportRun.Result imported(Map<String, Long> ids) {
        return new SteamImportRun.Result(ids.size(), 0, 0, ids.size(), ids.size(), ids, 0, false, false, null, 0);
    }

    @Test
    void gameAlreadyInTheCatalogIsJustFlagged() {
        when(mappings.findByPlatformAndPlatformGameIds(eq(Platform.STEAM), any()))
                .thenReturn(List.of(new GameMapping(1L, 50L, Platform.STEAM, "2767030", "Marvel Rivals")));

        List<ExemptionResult> results = service.exempt(List.of("2767030"));

        assertThat(results).containsExactly(new ExemptionResult("2767030", Status.EXEMPTED, 50L, null));
        verify(exemptions).setExempt(50L, true);
        verify(importRun, never()).run(any(), anyInt(), any(), anyLong());
    }

    @Test
    void rejectedGameIsForgottenThenImportedWithoutThePolicy() {
        when(importRun.run(List.of("2767030"), 1, ImportMode.EXEMPT, 0))
                .thenReturn(imported(Map.of("2767030", 77L)));

        List<ExemptionResult> results = service.exempt(List.of("2767030"));

        verify(failures).clearFailure(Platform.STEAM, "2767030");
        verify(exemptions).setExempt(77L, true);
        assertThat(results.get(0).status()).isEqualTo(Status.IMPORTED);
    }

    @Test
    void gameThatStillCannotBeImportedReportsWhy() {
        when(importRun.run(any(), anyInt(), any(), anyLong())).thenReturn(imported(Map.of()));
        when(failures.findReason(Platform.STEAM, "1")).thenReturn(Optional.of("no achievements"));

        List<ExemptionResult> results = service.exempt(List.of("1"));

        assertThat(results).containsExactly(new ExemptionResult("1", Status.NOT_IMPORTABLE, null, "no achievements"));
        verify(exemptions, never()).setExempt(anyLong(), eq(true));
    }

    @Test
    void removingAnUnknownAppReportsNotFound() {
        assertThat(service.removeExemption("123")).isFalse();
    }
}
