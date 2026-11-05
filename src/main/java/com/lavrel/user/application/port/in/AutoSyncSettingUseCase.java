package com.lavrel.user.application.port.in;

/** Whether the user's linked accounts are synced automatically (on by default). */
public interface AutoSyncSettingUseCase {

    boolean isEnabled(long userId);

    void setEnabled(long userId, boolean enabled);
}
