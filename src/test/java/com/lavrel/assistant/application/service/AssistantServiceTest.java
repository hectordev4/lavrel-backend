package com.lavrel.assistant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Answer;
import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Requester;
import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Status;
import com.lavrel.assistant.application.port.out.AchievementContextPort;
import com.lavrel.assistant.application.port.out.AssistantUsagePort;
import com.lavrel.assistant.application.port.out.GuideRepositoryPort;
import com.lavrel.assistant.application.port.out.LanguageModelPort;
import com.lavrel.assistant.application.port.out.ReferencePort;
import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.AchievementNotFoundException;
import com.lavrel.assistant.domain.AssistantQuotaExceededException;
import com.lavrel.assistant.domain.AssistantUnavailableException;
import com.lavrel.assistant.domain.ChatMessage;
import com.lavrel.assistant.domain.Excerpt;
import com.lavrel.assistant.domain.Guide;
import com.lavrel.assistant.domain.GuidePrompts;
import com.lavrel.catalog.domain.GameNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AssistantServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
    private static final Requester USER = new Requester(7L, false);
    private static final Requester ADMIN = new Requester(1L, true);

    private final AchievementContextPort achievements = mock(AchievementContextPort.class);
    private final GuideRepositoryPort guides = mock(GuideRepositoryPort.class);
    private final ReferencePort references = mock(ReferencePort.class);
    private final LanguageModelPort model = mock(LanguageModelPort.class);
    private final AssistantUsagePort usage = mock(AssistantUsagePort.class);
    private final AssistantService service = new AssistantService(achievements, guides, references, model, usage,
            Clock.fixed(NOW, ZoneOffset.UTC), 20, 400, 600, 40);

    private static AchievementContext achievement(boolean hidden) {
        return new AchievementContext(1L, "Hades", 10L, "Escape", "Escape the Underworld", hidden);
    }

    @BeforeEach
    void setUp() {
        when(achievements.find(1L, 10L)).thenReturn(Optional.of(achievement(false)));
        when(usage.tryConsume(anyString(), any(), anyInt())).thenReturn(true);
        when(model.complete(anyString(), anyString(), anyInt())).thenReturn("1. Do it.");
        when(model.modelName()).thenReturn("test-model");
        when(references.find(any())).thenReturn(List.of());
    }

    @Test
    void aFreshSavedGuideIsFreeAndSkipsTheModel() {
        when(guides.find(10L, "en")).thenReturn(Optional.of(
                new Guide(10L, "en", "Saved", true, List.of(), "m", NOW.minus(Duration.ofDays(1)), false)));

        Answer answer = service.guide(USER, 1L, 10L, false, null);

        assertThat(answer.saved()).isTrue();
        assertThat(answer.text()).isEqualTo("Saved");
        verify(usage, never()).tryConsume(anyString(), any(), anyInt());
        verify(model, never()).complete(anyString(), anyString(), anyInt());
    }

    @Test
    void aNewGuideCountsAndIsSavedForEveryone() {
        when(guides.find(10L, "en")).thenReturn(Optional.empty());

        Answer answer = service.guide(USER, 1L, 10L, false, "en");

        verify(usage).tryConsume(eq("user:7"), any(), eq(20));
        ArgumentCaptor<Guide> saved = ArgumentCaptor.forClass(Guide.class);
        verify(guides).save(saved.capture());
        assertThat(saved.getValue().grounded()).isFalse();
        assertThat(answer.saved()).isFalse();
    }

    @Test
    void groundedWhenReferenceTextWasUsed() {
        when(guides.find(10L, "en")).thenReturn(Optional.empty());
        when(references.find(any())).thenReturn(List.of(new Excerpt("Wiki", "https://w", "CC BY-SA 3.0", "text")));

        Answer answer = service.guide(USER, 1L, 10L, false, "en");

        assertThat(answer.grounded()).isTrue();
        assertThat(answer.sources()).extracting(Guide.Source::license).containsExactly("CC BY-SA 3.0");
    }

    @Test
    void hiddenAchievementsWaitForSpoilerConsent() {
        when(achievements.find(1L, 10L)).thenReturn(Optional.of(achievement(true)));

        Answer answer = service.guide(USER, 1L, 10L, false, "en");

        assertThat(answer.status()).isEqualTo(Status.SPOILER_CONSENT_REQUIRED);
        verify(model, never()).complete(anyString(), anyString(), anyInt());
    }

    @Test
    void thePlayersDailyLimitIsEnforced() {
        when(guides.find(10L, "en")).thenReturn(Optional.empty());
        when(usage.tryConsume(eq("user:7"), any(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.guide(USER, 1L, 10L, false, "en"))
                .isInstanceOf(AssistantQuotaExceededException.class);
        verify(model, never()).complete(anyString(), anyString(), anyInt());
    }

    @Test
    void theLimitForEveryoneProtectsTheFreeAllowance() {
        when(guides.find(10L, "en")).thenReturn(Optional.empty());
        when(usage.tryConsume(eq(AssistantService.EVERYONE), any(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.guide(USER, 1L, 10L, false, "en"))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void followUpsAreNeverSaved() {
        when(guides.find(10L, "en")).thenReturn(Optional.empty());

        service.ask(USER, 1L, 10L, "Is it missable?", false, "en");

        verify(guides, never()).save(any());
    }

    @Test
    void unknownAchievementsAreNotFound() {
        when(achievements.find(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.guide(USER, 1L, 99L, false, "en"))
                .isInstanceOf(AchievementNotFoundException.class);
    }

    @Test
    void adminsAreNeverStoppedByTheDailyLimits() {
        when(guides.find(10L, "en")).thenReturn(Optional.empty());
        when(usage.tryConsume(anyString(), any(), anyInt())).thenReturn(false);

        Answer answer = service.guide(ADMIN, 1L, 10L, false, "en");

        assertThat(answer.text()).isEqualTo("1. Do it.");
        verify(usage, never()).tryConsume(eq(AssistantService.EVERYONE), any(), anyInt());
        assertThat(service.usage(ADMIN).unlimited()).isTrue();
        assertThat(service.usage(ADMIN).dailyLimit()).isNull();
    }

    @Test
    void curatedGuidesNeverExpire() {
        when(guides.find(10L, "en")).thenReturn(Optional.of(
                new Guide(10L, "en", "Reviewed", false, List.of(), "m", NOW.minus(Duration.ofDays(400)), true)));

        Answer answer = service.guide(USER, 1L, 10L, false, "en");

        assertThat(answer.saved()).isTrue();
        assertThat(answer.curated()).isTrue();
    }

    @Test
    void chatSendsTheConversationWithReferencesAndCounts() {
        when(achievements.publishedGameTitle(1L)).thenReturn(Optional.of("Hades"));
        when(references.findForGame(1L, "Hades", "Best weapon?"))
                .thenReturn(List.of(new Excerpt("Weapons", "https://w/Weapons", "CC BY-SA 3.0", "Stygius...")));
        when(model.chat(anyString(), any(), anyInt())).thenReturn("The Stygian Blade!");

        Answer answer = service.chat(USER, 1L, List.of(
                new ChatMessage(ChatMessage.Role.USER, "Hi"),
                new ChatMessage(ChatMessage.Role.ASSISTANT, "Hello!"),
                new ChatMessage(ChatMessage.Role.USER, "Best weapon?")), "en");

        assertThat(answer.grounded()).isTrue();
        assertThat(answer.sources()).extracting(Guide.Source::url).containsExactly("https://w/Weapons");
        verify(usage).tryConsume(eq("user:7"), any(), eq(20));
        verify(guides, never()).save(any());
    }

    @Test
    void chatOnlyAboutPublishedGames() {
        when(achievements.publishedGameTitle(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.chat(USER, 9L,
                List.of(new ChatMessage(ChatMessage.Role.USER, "Hi")), "en"))
                .isInstanceOf(GameNotFoundException.class);
    }

    // ---- Only answers the player receives are counted ----

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @Test
    void aFailedNewGuideGivesBackBothCounts() {
        when(guides.find(10L, "en")).thenReturn(Optional.empty());
        when(model.complete(anyString(), anyString(), anyInt()))
                .thenThrow(new AssistantUnavailableException("Cloudflare allowance used up", null));

        assertThatThrownBy(() -> service.guide(USER, 1L, 10L, false, "en"))
                .isInstanceOf(AssistantUnavailableException.class);

        verify(usage).release("user:7", TODAY);
        verify(usage).release("all", TODAY);
        verify(guides, never()).save(any());
    }

    @Test
    void aFailedFollowUpGivesBackBothCounts() {
        when(model.complete(anyString(), anyString(), anyInt()))
                .thenThrow(new AssistantUnavailableException("Model retired", null));

        assertThatThrownBy(() -> service.ask(USER, 1L, 10L, "Where is it?", false, "en"))
                .isInstanceOf(AssistantUnavailableException.class);

        verify(usage).release("user:7", TODAY);
        verify(usage).release("all", TODAY);
    }

    @Test
    void aFailedChatAnswerGivesBackBothCounts() {
        when(achievements.publishedGameTitle(1L)).thenReturn(Optional.of("Hades"));
        when(references.findForGame(anyLong(), anyString(), anyString())).thenReturn(List.of());
        when(model.chat(anyString(), any(), anyInt()))
                .thenThrow(new AssistantUnavailableException("Empty answer", null));

        assertThatThrownBy(() -> service.chat(USER, 1L,
                List.of(new ChatMessage(ChatMessage.Role.USER, "Best weapon?")), "en"))
                .isInstanceOf(AssistantUnavailableException.class);

        verify(usage).release("user:7", TODAY);
        verify(usage).release("all", TODAY);
    }

    @Test
    void reachingTheGlobalLimitGivesBackThePlayersCount() {
        when(guides.find(10L, "en")).thenReturn(Optional.empty());
        when(usage.tryConsume(eq("all"), any(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.guide(USER, 1L, 10L, false, "en"))
                .isInstanceOf(AssistantUnavailableException.class);

        verify(usage).release("user:7", TODAY);
        verify(usage, never()).release(eq("all"), any());
        verify(model, never()).complete(anyString(), anyString(), anyInt());
    }

    @Test
    void aPlayerOverTheirLimitHasNothingToGiveBack() {
        when(usage.tryConsume(eq("user:7"), any(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.ask(USER, 1L, 10L, "Where is it?", false, "en"))
                .isInstanceOf(AssistantQuotaExceededException.class);

        verify(usage, never()).release(anyString(), any());
        verify(usage, never()).tryConsume(eq("all"), any(), anyInt());
    }

    @Test
    void aSuccessfulAnswerKeepsBothCounts() {
        when(guides.find(10L, "en")).thenReturn(Optional.empty());

        service.guide(USER, 1L, 10L, false, "en");

        verify(usage).tryConsume("user:7", TODAY, 20);
        verify(usage).tryConsume("all", TODAY, 400);
        verify(usage, never()).release(anyString(), any());
    }

    @Test
    void aSavedGuideNeverCountsAndNeverGivesBack() {
        when(guides.find(10L, "en")).thenReturn(Optional.of(
                new Guide(10L, "en", "Saved", true, List.of(), "m", NOW.minus(Duration.ofDays(1)), false)));

        service.guide(USER, 1L, 10L, false, "en");

        verifyNoMoreInteractions(usage);
    }

    @Test
    void aFailedAnswerForAnAdminGivesBackOnlyTheirOwnCount() {
        when(model.complete(anyString(), anyString(), anyInt()))
                .thenThrow(new AssistantUnavailableException("Timeout", null));

        assertThatThrownBy(() -> service.ask(ADMIN, 1L, 10L, "Where is it?", false, "en"))
                .isInstanceOf(AssistantUnavailableException.class);

        verify(usage).release("user:1", TODAY);
        verify(usage, never()).release(eq("all"), any());
    }

    @Test
    void aRequestThatCrossesMidnightGivesBackTheDayItCounted() {
        // The clock reads 23:59:59 until the counting is done, then the next day.
        Deque<Instant> times = new ArrayDeque<>(List.of(
                Instant.parse("2026-11-06T23:59:59Z"), Instant.parse("2026-11-06T23:59:59Z")));
        Clock crossing = new Clock() {
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() {
                return times.isEmpty() ? Instant.parse("2026-11-07T00:00:05Z") : times.poll();
            }
        };
        AssistantService late = new AssistantService(achievements, guides, references, model, usage, crossing,
                20, 400, 600, 40);
        when(guides.find(10L, "en")).thenReturn(Optional.empty());
        when(model.complete(anyString(), anyString(), anyInt()))
                .thenThrow(new AssistantUnavailableException("Timeout", null));

        assertThatThrownBy(() -> late.guide(USER, 1L, 10L, false, "en"))
                .isInstanceOf(AssistantUnavailableException.class);

        verify(usage).tryConsume("user:7", TODAY, 20);
        verify(usage).release("user:7", TODAY);
        verify(usage).release("all", TODAY);
    }

    // ---- English search terms for the game's wiki (chat) ----

    private static final String SPANISH = "¿Dónde consigo el anillo de la serpiente de oro?";

    private void aChatAboutDarkSouls(boolean hasWiki) {
        when(achievements.publishedGameTitle(5L)).thenReturn(Optional.of("Dark Souls III"));
        when(references.hasSource(5L)).thenReturn(hasWiki);
        when(references.findForGame(anyLong(), anyString(), anyString())).thenReturn(List.of(
                new Excerpt("Covetous Gold Serpent Ring", "https://w/CGSR", "CC BY-SA 3.0", "Found in...")));
        when(model.chat(anyString(), any(), anyInt())).thenReturn("En la Ciudad de Irithyll...");
    }

    @Test
    void theWikiIsSearchedWithEnglishTermsMadeFromTheQuestion() {
        aChatAboutDarkSouls(true);
        when(model.complete(eq(GuidePrompts.searchTermsSystem()), anyString(), eq(40)))
                .thenReturn("Covetous Gold Serpent Ring location");

        Answer answer = service.chat(USER, 5L, List.of(new ChatMessage(ChatMessage.Role.USER, SPANISH)), "es");

        verify(model).complete(GuidePrompts.searchTermsSystem(),
                GuidePrompts.searchTermsQuestion("Dark Souls III", SPANISH), 40);
        verify(references).findForGame(5L, "Dark Souls III", "Covetous Gold Serpent Ring location");
        assertThat(answer.grounded()).isTrue();
    }

    @Test
    void ifTheSearchTermsCallFailsTheQuestionIsSearchedAsItIs() {
        aChatAboutDarkSouls(true);
        when(model.complete(eq(GuidePrompts.searchTermsSystem()), anyString(), anyInt()))
                .thenThrow(new AssistantUnavailableException("Timeout", null));

        Answer answer = service.chat(USER, 5L, List.of(new ChatMessage(ChatMessage.Role.USER, SPANISH)), "es");

        verify(references).findForGame(5L, "Dark Souls III", SPANISH);
        assertThat(answer.text()).isEqualTo("En la Ciudad de Irithyll...");
        verify(usage, never()).release(anyString(), any());
    }

    @Test
    void withoutAnApprovedWikiNoSearchTermsAreMade() {
        aChatAboutDarkSouls(false);

        service.chat(USER, 5L, List.of(new ChatMessage(ChatMessage.Role.USER, SPANISH)), "es");

        verify(model, never()).complete(anyString(), anyString(), anyInt());
        verify(references).findForGame(5L, "Dark Souls III", SPANISH);
    }

    @Test
    void theSearchTermsCallIsNotCounted() {
        aChatAboutDarkSouls(true);
        when(model.complete(eq(GuidePrompts.searchTermsSystem()), anyString(), anyInt()))
                .thenReturn("Covetous Gold Serpent Ring location");

        service.chat(USER, 5L, List.of(new ChatMessage(ChatMessage.Role.USER, SPANISH)), "es");

        verify(usage, times(1)).tryConsume(eq("user:7"), any(), anyInt());
        verify(usage, times(1)).tryConsume(eq("all"), any(), anyInt());
    }

    @Test
    void theAnswerUsesTheGroundedRulesOnlyWithReferenceText() {
        aChatAboutDarkSouls(false);
        when(references.findForGame(anyLong(), anyString(), anyString())).thenReturn(List.of());

        Answer answer = service.chat(USER, 5L, List.of(new ChatMessage(ChatMessage.Role.USER, SPANISH)), "es");

        verify(model).chat(eq(GuidePrompts.chatSystem("es", "Dark Souls III", false)), any(), anyInt());
        assertThat(answer.grounded()).isFalse();
    }
}
