package com.lavrel.assistant.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavrel.assistant.domain.AssistantUnavailableException;
import com.lavrel.assistant.domain.ChatMessage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkersAiModelTest {

    private final WorkersAiModel model = new WorkersAiModel("", "", "@cf/google/gemma-4-26b-a4b-it");

    @Test
    void theRequestTurnsThinkingOffAndUsesMaxCompletionTokens() {
        Map<String, Object> body = model.body("rules", List.of(new ChatMessage(ChatMessage.Role.USER, "Hi")), 1400);

        assertThat(body).containsEntry("model", "@cf/google/gemma-4-26b-a4b-it")
                .containsEntry("max_completion_tokens", 1400)
                .containsEntry("chat_template_kwargs", Map.of("enable_thinking", false))
                .doesNotContainKey("max_tokens");
        assertThat(body.get("messages")).isEqualTo(List.of(
                Map.of("role", "system", "content", "rules"),
                Map.of("role", "user", "content", "Hi")));
    }

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
    void onlyReasoningAndNoContentIsNoAnswer() {
        String thinking = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                + "\"reasoning\":\"The player wants...\"},\"finish_reason\":\"length\"}]}";

        assertThatThrownBy(() -> model.extract(thinking))
                .isInstanceOf(AssistantUnavailableException.class)
                .hasMessageContaining("only its reasoning");
    }

    @Test
    void unreadableJsonIsNoAnswer() {
        assertThatThrownBy(() -> model.extract("not json")).isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void withoutCredentialsItSaysSoInsteadOfCalling() {
        assertThatThrownBy(() -> model.complete("s", "u", 10)).isInstanceOf(AssistantUnavailableException.class);
    }
}
