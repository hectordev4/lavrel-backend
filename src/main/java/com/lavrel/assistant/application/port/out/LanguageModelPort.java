package com.lavrel.assistant.application.port.out;

import com.lavrel.assistant.domain.AssistantUnavailableException;
import com.lavrel.assistant.domain.ChatMessage;
import java.util.List;

/** A text model (Cloudflare Workers AI today). Text in, text out: no tools, no access to anything else. */
public interface LanguageModelPort {

    /** @throws AssistantUnavailableException if it is not configured, out of free allowance, or failing */
    default String complete(String system, String user, int maxTokens) {
        return chat(system, List.of(new ChatMessage(ChatMessage.Role.USER, user)), maxTokens);
    }

    /** A conversation: the system rules, then the messages in order (the last one is the player's). */
    String chat(String system, List<ChatMessage> messages, int maxTokens);

    /** Which model answers, saved with each guide. */
    String modelName();
}
