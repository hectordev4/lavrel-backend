package com.lavrel.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavrel.assistant.domain.ChatMessage.Role;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatMessageTest {

    @Test
    void theConversationMustEndWithTheQuestion() {
        assertThatThrownBy(() -> ChatMessage.validConversation(List.of(new ChatMessage(Role.ASSISTANT, "Hi"))))
                .isInstanceOf(InvalidQuestionException.class);
        assertThatThrownBy(() -> ChatMessage.validConversation(List.of()))
                .isInstanceOf(InvalidQuestionException.class);
    }

    @Test
    void onlyTheLastMessagesAreKept() {
        List<ChatMessage> long_ = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            long_.add(new ChatMessage(i % 2 == 0 ? Role.USER : Role.ASSISTANT, "m" + i));
        }
        long_.add(new ChatMessage(Role.USER, "last"));
        List<ChatMessage> kept = ChatMessage.validConversation(long_);
        assertThat(kept).hasSize(ChatMessage.MAX_MESSAGES);
        assertThat(kept.get(kept.size() - 1).content()).isEqualTo("last");
    }

    @Test
    void playerMessagesAreCleanedAndLimited() {
        assertThat(ChatMessage.validConversation(List.of(new ChatMessage(Role.USER, "  where   is it? ")))
                .get(0).content()).isEqualTo("where is it?");
        assertThatThrownBy(() -> ChatMessage.validConversation(List.of(new ChatMessage(Role.USER, "x".repeat(501)))))
                .isInstanceOf(InvalidQuestionException.class);
    }

    @Test
    void everyPlayerMessageInTheLast12MustBeAValidQuestion() {
        assertThatThrownBy(() -> ChatMessage.validConversation(List.of(
                new ChatMessage(Role.USER, "x".repeat(501)),
                new ChatMessage(Role.ASSISTANT, "Hello!"),
                new ChatMessage(Role.USER, "Best weapon?")))).isInstanceOf(InvalidQuestionException.class);
    }

    @Test
    void longEarlierAnswersAreCutNotRefused() {
        List<ChatMessage> kept = ChatMessage.validConversation(List.of(
                new ChatMessage(Role.USER, "All bosses?"),
                new ChatMessage(Role.ASSISTANT, "a".repeat(9000)),
                new ChatMessage(Role.USER, "And the first one?")));

        assertThat(kept.get(1).content()).hasSize(ChatMessage.MAX_ANSWER_LENGTH + 1).endsWith("\u2026");
        assertThat(ChatMessage.MAX_MESSAGE_LENGTH).isEqualTo(20_000);
    }
}
