package com.lavrel.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.catalog.application.port.in.ManageFranchisesUseCase.CleanupReport;
import com.lavrel.catalog.application.port.out.FranchiseBlocklistPort;
import com.lavrel.catalog.application.port.out.FranchiseLinksPort;
import com.lavrel.catalog.application.port.out.FranchiseLinksPort.FranchiseLink;
import com.lavrel.catalog.domain.CreatorNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManageFranchisesServiceTest {

    private final FranchiseLinksPort links = mock(FranchiseLinksPort.class);
    private final FranchiseBlocklistPort blocklist = mock(FranchiseBlocklistPort.class);
    private final ManageFranchisesService service =
            new ManageFranchisesService(links, blocklist, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        when(blocklist.keys()).thenReturn(Set.of());
        when(links.findAll()).thenReturn(List.of(
                new FranchiseLink(1L, 10L, "Team17 Digital", List.of("Team17"), List.of("Team17")),
                new FranchiseLink(2L, 10L, "Team17 Digital", List.of("Team17"), List.of("Team17")),
                new FranchiseLink(3L, 20L, "Worms", List.of("Team17"), List.of("Team17"))));
    }

    @Test
    void aDryRunOnlyReports() {
        CleanupReport report = service.cleanup(false);

        assertThat(report.findings()).singleElement().satisfies(f -> {
            assertThat(f.name()).isEqualTo("Team17 Digital");
            assertThat(f.games()).isEqualTo(2);
        });
        verify(links, never()).unlink(any());
    }

    @Test
    void applyingRemovesOnlyTheRejectedLinks() {
        when(links.unlink(any())).thenReturn(2);

        CleanupReport report = service.cleanup(true);

        assertThat(report.linksRemoved()).isEqualTo(2);
        verify(links).unlink(eq(List.of(
                new FranchiseLink(1L, 10L, "Team17 Digital", List.of("Team17"), List.of("Team17")),
                new FranchiseLink(2L, 10L, "Team17 Digital", List.of("Team17"), List.of("Team17")))));
    }

    @Test
    void blockingRemembersTheNameAndRemovesItsLinks() {
        when(links.creatorName(30L)).thenReturn(Optional.of("FRANCHISE"));

        service.block(30L);

        verify(blocklist).add(eq("franchise"), eq("FRANCHISE"), any());
        verify(links).unlinkCreator(30L);
    }

    @Test
    void unknownCreatorsCannotBeBlocked() {
        when(links.creatorName(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.block(99L)).isInstanceOf(CreatorNotFoundException.class);
        verify(links, never()).unlinkCreator(anyLong());
    }
}
