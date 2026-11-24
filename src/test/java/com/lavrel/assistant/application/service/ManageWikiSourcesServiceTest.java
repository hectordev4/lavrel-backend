package com.lavrel.assistant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.assistant.application.port.in.ManageWikiSourcesUseCase.GameWiki;
import com.lavrel.assistant.application.port.out.GuideRepositoryPort;
import com.lavrel.assistant.application.port.out.ReferenceWikiRepositoryPort;
import com.lavrel.assistant.application.port.out.WikiClientPort;
import com.lavrel.assistant.domain.InvalidWikiSourceException;
import com.lavrel.assistant.domain.ReferenceWiki;
import com.lavrel.assistant.domain.WikiLicenseNotAllowedException;
import com.lavrel.assistant.domain.WikiSiteInfo;
import com.lavrel.assistant.domain.WikiUnreachableException;
import com.lavrel.catalog.application.port.in.CheckPublishedGameUseCase;
import com.lavrel.catalog.domain.GameNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ManageWikiSourcesServiceTest {

    private static final String DARK_SOULS = "https://darksouls.fandom.com/api.php";
    private static final WikiSiteInfo DARK_SOULS_INFO = new WikiSiteInfo("Dark Souls Wiki",
            "https://darksouls.fandom.com", "https://creativecommons.org/licenses/by-sa/3.0/", "CC-BY-SA",
            "https://darksouls.fandom.com/wiki/$1", 4200);

    private final ReferenceWikiRepositoryPort wikis = mock(ReferenceWikiRepositoryPort.class);
    private final WikiClientPort client = mock(WikiClientPort.class);
    private final GuideRepositoryPort guides = mock(GuideRepositoryPort.class);
    private final CheckPublishedGameUseCase catalog = mock(CheckPublishedGameUseCase.class);
    private final ManageWikiSourcesService service = new ManageWikiSourcesService(wikis, client, guides, catalog,
            new TransactionTemplate(mock(PlatformTransactionManager.class)),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @BeforeEach
    void savingGivesTheWikiAnId() {
        when(wikis.save(any())).thenAnswer(i -> {
            ReferenceWiki w = i.getArgument(0);
            return w.id() == null ? w.withId(7L) : w;
        });
        when(wikis.findByApiUrl(any())).thenReturn(Optional.empty());
        when(wikis.findForGame(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void aNewWikiIsStoredWithWhatItSaysAboutItselfAndLinked() {
        when(client.siteInfo(DARK_SOULS)).thenReturn(Optional.of(DARK_SOULS_INFO));
        when(guides.deleteByGame(18L)).thenReturn(3);

        GameWiki result = service.approve(18L, "https://DarkSouls.fandom.com/api.php");

        assertThat(result.wiki().id()).isEqualTo(7L);
        assertThat(result.wiki().siteName()).isEqualTo("Dark Souls Wiki");
        assertThat(result.wiki().license()).isEqualTo("CC BY-SA 3.0");
        assertThat(result.wiki().licenseUrl()).isEqualTo("https://creativecommons.org/licenses/by-sa/3.0/");
        assertThat(result.guidesForgotten()).isEqualTo(3);
        verify(wikis).link(18L, 7L, Instant.EPOCH);
        verify(wikis).deleteUnused();
    }

    @Test
    void aSecondGameOfTheFranchiseReusesTheStoredWiki() {
        ReferenceWiki stored = new ReferenceWiki(7L, DARK_SOULS, null, "CC BY-SA 3.0", null, null, Instant.EPOCH);
        when(wikis.findByApiUrl(DARK_SOULS)).thenReturn(Optional.of(stored));
        when(client.siteInfo(DARK_SOULS)).thenReturn(Optional.of(DARK_SOULS_INFO));

        GameWiki result = service.approve(19L, DARK_SOULS);

        assertThat(result.wiki().id()).isEqualTo(7L);
        assertThat(result.wiki().siteName()).as("filled in from the wiki").isEqualTo("Dark Souls Wiki");
        verify(wikis).link(19L, 7L, Instant.EPOCH);
    }

    @Test
    void approvingTheSameWikiAgainKeepsTheGamesSavedGuides() {
        ReferenceWiki stored = new ReferenceWiki(7L, DARK_SOULS, "Dark Souls Wiki", "CC BY-SA 3.0", null, null,
                Instant.EPOCH);
        when(wikis.findByApiUrl(DARK_SOULS)).thenReturn(Optional.of(stored));
        when(wikis.findForGame(18L)).thenReturn(Optional.of(stored));
        when(client.siteInfo(DARK_SOULS)).thenReturn(Optional.of(DARK_SOULS_INFO));

        GameWiki result = service.approve(18L, DARK_SOULS);

        assertThat(result.guidesForgotten()).isZero();
        verify(guides, never()).deleteByGame(anyLong());
        verify(wikis, never()).link(anyLong(), anyLong(), any());
    }

    @Test
    void aNonCommercialLicenseIsRefusedAndNothingIsStored() {
        when(client.siteInfo(DARK_SOULS)).thenReturn(Optional.of(new WikiSiteInfo("Some Wiki",
                "https://darksouls.fandom.com", "https://creativecommons.org/licenses/by-nc-sa/3.0/",
                "CC BY-NC-SA", null, 100)));

        assertThatThrownBy(() -> service.approve(18L, DARK_SOULS))
                .isInstanceOf(WikiLicenseNotAllowedException.class);
        verify(wikis, never()).save(any());
        verify(wikis, never()).link(anyLong(), anyLong(), any());
    }

    @Test
    void aWikiThatDoesNotAnswerIsRefused() {
        when(client.siteInfo(DARK_SOULS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(18L, DARK_SOULS)).isInstanceOf(WikiUnreachableException.class);
        verify(wikis, never()).save(any());
    }

    @Test
    void aWikiServedFromAnotherHostIsRefused() {
        when(client.siteInfo(DARK_SOULS)).thenReturn(Optional.of(new WikiSiteInfo("Dark Souls Wiki",
                "https://darksouls.wiki.gg", "https://creativecommons.org/licenses/by-sa/3.0/", "CC-BY-SA", null, 10)));

        assertThatThrownBy(() -> service.approve(18L, DARK_SOULS))
                .isInstanceOf(InvalidWikiSourceException.class).hasMessageContaining("darksouls.wiki.gg");
    }

    @Test
    void onlyPublishedGamesAndNoCallToTheWikiOtherwise() {
        doThrow(new GameNotFoundException(9L)).when(catalog).requirePublished(9L);

        assertThatThrownBy(() -> service.approve(9L, DARK_SOULS)).isInstanceOf(GameNotFoundException.class);
        verify(client, never()).siteInfo(any());
    }

    @Test
    void removingAWikiForgetsTheGamesGuidesAndUnusedWikis() {
        when(wikis.unlink(1L)).thenReturn(true);

        service.remove(1L);

        verify(guides).deleteByGame(1L);
        verify(wikis).deleteUnused();
    }

    @Test
    void removingAMissingWikiChangesNothing() {
        when(wikis.unlink(2L)).thenReturn(false);

        service.remove(2L);

        verify(guides, never()).deleteByGame(anyLong());
    }
}
