package com.lavrel.library.application.port.in;

/** One round of the automatic sync (run by a scheduler, not over HTTP). */
public interface AutoSyncUseCase {

    AutoSyncReport runOnce();

    /** stoppedEarly: the platform was unavailable or rate-limited us, so the rest waits for the next round. */
    record AutoSyncReport(int accountsSynced, int accountsFailed, int unlocksRecorded, boolean stoppedEarly) {
    }
}
