package com.lavrel.assistant.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavrel.assistant.domain.AssistantUnavailableException;
import org.junit.jupiter.api.Test;

class WorkersAiModelTest {

    private final WorkersAiModel model = new WorkersAiModel("", "", "@cf/meta/llama-3.1-8b-instruct");

    @Test
    void readsTheOpenAiFormat() {
        assertThat(model.extract("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hi\"}}]}"))
                .isEqualTo("Hi");
    }

    @Test
    void readsWorkersAiOwnFormat() {
        assertThat(model.extract("{\"result\":{\"response\":\"Hi\"},\"success\":true}")).isEqualTo("Hi");
    }

    @Test
    void withoutCredentialsItSaysSoInsteadOfCalling() {
        assertThatThrownBy(() -> model.complete("s", "u", 10)).isInstanceOf(AssistantUnavailableException.class);
    }
}
