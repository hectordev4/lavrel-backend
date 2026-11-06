package com.lavrel.assistant.application.port.out;

import java.time.LocalDate;

/** Daily counters (assistant_usage): one per user, one for everybody together. */
public interface AssistantUsagePort {

    /** Adds one if the counter is below the limit. @return false (and nothing added) if it is already at the limit */
    boolean tryConsume(String scope, LocalDate day, int limit);

    int used(String scope, LocalDate day);
}
