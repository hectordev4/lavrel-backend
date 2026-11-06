package com.lavrel.assistant.domain;

/** The player's daily number of new answers is used up. Saved guides are not counted. */
public class AssistantQuotaExceededException extends RuntimeException {

    public AssistantQuotaExceededException(int dailyLimit) {
        super("You have used today's " + dailyLimit + " assistant answers. Saved guides still work; new ones tomorrow.");
    }
}
