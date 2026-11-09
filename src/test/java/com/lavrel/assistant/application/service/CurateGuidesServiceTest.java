package com.lavrel.assistant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Answer;
import com.lavrel.assistant.application.port.out.AchievementContextPort;
import com.lavrel.assistant.application.port.out.GuideRepositoryPort;
import com.lavrel.assistant.application.port.out.LanguageModelPort;
import com.lavrel.assistant.application.port.out.ReferencePort;
import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.Guide;
import com.lavrel.assistant.domain.InvalidQuestionException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CurateGuidesServiceTest {

    private final AchievementContextPort achievements = mock(AchievementContextPort.class);
    private final GuideRepositoryPort guides = mock(GuideRepositoryPort.class);
    private final ReferencePort references = mock(ReferencePort.class);
    private final LanguageModelPort model = mock(LanguageModelPort.class);
    private final CurateGuidesService service = new CurateGuidesService(achievements, guides, references, model,
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), 1500);

    @BeforeEach
    void setUp() {
        when(achievements.findById(10L)).thenReturn(Optional.of(
                new AchievementContext(1L, "Hades", 10L, "Escape", "Escape", false)));
        when(references.find(any())).thenReturn(List.of());
        when(model.modelName()).thenReturn("m");
    }

    @Test
    void aRewriteFollowsTheInstructionsAndIsSavedAsCurated() {
        when(model.complete(anyString(), contains("More detail on Elysium"), anyInt())).thenReturn("Detailed guide");

        Answer answer = service.rewrite(10L, "en", "More detail on Elysium");

        ArgumentCaptor<Guide> saved = ArgumentCaptor.forClass(Guide.class);
        verify(guides).save(saved.capture());
        assertThat(saved.getValue().curated()).isTrue();
        assertThat(answer.text()).isEqualTo("Detailed guide");
    }

    @Test
    void anEditKeepsTheSourcesOfThePreviousGuide() {
        Guide previous = new Guide(10L, "en", "Old", true,
                List.of(new Guide.Source("Wiki", "https://w", "CC BY-SA 3.0")), "m", Instant.EPOCH, false);
        when(guides.find(10L, "en")).thenReturn(Optional.of(previous));

        Answer answer = service.edit(10L, "en", "  My own guide  ");

        assertThat(answer.text()).isEqualTo("My own guide");
        assertThat(answer.sources()).hasSize(1);
        assertThat(answer.curated()).isTrue();
    }

    @Test
    void instructionsAreRequired() {
        assertThatThrownBy(() -> service.rewrite(10L, "en", " ")).isInstanceOf(InvalidQuestionException.class);
        verify(guides, never()).save(any());
    }
}
