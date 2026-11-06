package com.lavrel.assistant.application.port.out;

import com.lavrel.assistant.domain.AssistantUnavailableException;

/** A text model (Cloudflare Workers AI today). Text in, text out: no tools, no access to anything else. */
public interface LanguageModelPort {

    /** @throws AssistantUnavailableException if it is not configured, out of free allowance, or failing */
    String complete(String system, String user, int maxTokens);

    /** Which model answers, saved with each guide. */
    String modelName();
}
