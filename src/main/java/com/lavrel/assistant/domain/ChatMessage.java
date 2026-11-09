package com.lavrel.assistant.domain;

import java.util.List;
import java.util.Objects;

/**
 * One message of an open chat about a game. The conversation lives in the frontend and is sent with each question:
 * nothing of it is stored by the backend.
 */
public record ChatMessage(Role role, String content) {

    public static final int MAX_MESSAGES = 12;
    public static final int MAX_ANSWER_LENGTH = 4000;

    public enum Role {
        USER, ASSISTANT
    }

    public ChatMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(content, "content");
    }

    /**
     * Keeps the last {@value #MAX_MESSAGES} messages, cleans the player's ones like any question, and requires the last
     * one to be the player's.
     * @throws InvalidQuestionException if the conversation is empty, does not end with a question, or has a message
     *                                  that is too long
     */
    public static List<ChatMessage> validConversation(List<ChatMessage> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new InvalidQuestionException("Ask something about this game");
        }
        List<ChatMessage> recent = raw.size() > MAX_MESSAGES ? raw.subList(raw.size() - MAX_MESSAGES, raw.size()) : raw;
        if (recent.get(recent.size() - 1).role() != Role.USER) {
            throw new InvalidQuestionException("The conversation must end with the player's question");
        }
        return recent.stream().map(m -> {
            if (m.role() == Role.USER) {
                return new ChatMessage(Role.USER, Questions.clean(m.content()));
            }
            if (m.content().length() > MAX_ANSWER_LENGTH) {
                throw new InvalidQuestionException("An earlier answer is too long to send back");
            }
            return m;
        }).toList();
    }
}
