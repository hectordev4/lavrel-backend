package com.lavrel.assistant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.lavrel.catalog.domain.GameNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
            Clock.fixed(NOW, ZoneOffset.UTC), 20, 400, 600);

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
        when(references.findForGame(1L, "Best weapon?"))
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
}
