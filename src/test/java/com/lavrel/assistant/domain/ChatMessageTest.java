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
}
