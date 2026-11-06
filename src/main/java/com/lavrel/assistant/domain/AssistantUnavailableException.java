package com.lavrel.assistant.domain;

/** The model cannot answer now: not configured, its free daily allowance is used up, or it failed. */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
